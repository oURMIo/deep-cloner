package com.home.playground.service

import com.home.playground.exception.CloningException
import com.home.playground.service.cloning.CloningStrategy
import com.home.playground.service.cloning.DeepCloner
import com.home.playground.service.cloning.Freezable
import com.home.playground.service.cloning.InstantiationStrategy
import com.home.playground.service.cloning.Immutable
import com.home.playground.service.cloning.ObjenesisInstantiationStrategy
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.IdentityHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class ClonerService {
    private val instantiationStrategy: InstantiationStrategy = ObjenesisInstantiationStrategy.getInstance()
    private val nullInsteadFieldAnnotations = mutableSetOf<Class<*>>()
    private val fieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()
    private val cloningStrategies: MutableList<CloningStrategy>? = null

    private val cloners = ConcurrentHashMap<Class<*>, DeepCloner>()
    private val immutables = ConcurrentHashMap<Class<*>, Boolean>()

    fun <T> clone(obj: T): T {
        return deepCopyHelper(obj, IdentityHashMap())
    }

    private fun <T> deepCopyHelper(obj: T, visited: MutableMap<Any, Any>): T {
        if (obj == null) return obj
        if (isPrimitiveOrWrapper(obj!!::class.java)) return obj

        visited[obj]?.let { return it as T }

        if (obj.javaClass.isArray) {
            return copyArray(obj, visited) as T
        }

        if (obj is Map<*, *>) {
            return copyMap(obj, visited) as T
        }

        if (obj is Collection<*>) {
            return copyCollection(obj, visited) as T
        }

        return try {
            viewAs(obj, visited)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deep copy object", e)
        }
    }

    private fun <T> fastCloneOrNewInstance(c: Class<T>): T {
        return instantiationStrategy.newInstance(c)
    }

    private fun <T> copyPropertiesOfInheritedClass(src: T, est: T, visited: MutableMap<Any, Any>) {
        requireNotNull(src) { "src can't be null" }
        requireNotNull(est) { "est can't be null" }
        val srcClz = src.javaClass
        val estClz = est.javaClass

        val fields = allFields(srcClz)
        val estFields = allFields(estClz)
        fields.filterNot { Modifier.isStatic(it.modifiers) }.forEach { field ->
            try {
                field.isAccessible = true
                val fieldObject = field[src]
                if (field in estFields) {
                    field[est] = deepCopyHelper(fieldObject, visited)
                }
            } catch (e: IllegalArgumentException) {
                throw CloningException(e)
            } catch (e: IllegalAccessException) {
                throw CloningException(e)
            }
        }
    }

    private fun <T> viewAs(o: T, visited: MutableMap<Any, Any>): T {
        val clazz = o!!::class.java
        val newInstance = fastCloneOrNewInstance(clazz) as T
        copyPropertiesOfInheritedClass(o, newInstance, visited)
        return newInstance
    }

    private fun <T> cloneInternal(o: T?, clones: MutableMap<Any?, Any?>?): T? {
        if (o == null || o === this) return null
        clones?.get(o)?.let { return it as T? }

        val aClass = o!!::class.java
        val cloner = cloners.computeIfAbsent(aClass) { findDeepCloner(aClass) }

        return when (cloner) {
            IGNORE_CLONER -> o
            NULL_CLONER -> null
            else -> cloner.deepClone(o, clones)
        }
    }

    private fun isPrimitiveOrWrapper(clazz: Class<*>): Boolean {
        return clazz.isPrimitive ||
                clazz == java.lang.Boolean::class.java || clazz == java.lang.Integer::class.java ||
                clazz == java.lang.Character::class.java || clazz == java.lang.Byte::class.java ||
                clazz == java.lang.Short::class.java || clazz == java.lang.Double::class.java ||
                clazz == java.lang.Long::class.java || clazz == java.lang.Float::class.java ||
                clazz == String::class.java
    }

    private fun copyArray(array: Any, visited: MutableMap<Any, Any>): Any {
        val length = java.lang.reflect.Array.getLength(array)
        val componentType = array.javaClass.componentType
        val newArray = java.lang.reflect.Array.newInstance(componentType, length)
        visited[array] = newArray
        for (i in 0 until length) {
            java.lang.reflect.Array.set(newArray, i, deepCopyHelper(java.lang.reflect.Array.get(array, i), visited))
        }
        return newArray
    }

    private fun copyCollection(collection: Collection<*>, visited: MutableMap<Any, Any>): Collection<*> {
        val newCollection: Collection<Any> = when (collection) {
            is List<*> -> ArrayList()
            is TreeSet<*> -> TreeSet()
            is Set<*> -> HashSet()
            else -> throw IllegalArgumentException("Unsupported collection type: ${collection::class.java}")
        }
        visited[collection] = newCollection
        for (item in collection) {
            deepCopyHelper(item, visited)?.let { (newCollection as MutableCollection<Any>).add(it) }
        }
        return newCollection
    }

    private fun copyMap(map: Map<*, *>, visited: MutableMap<Any, Any>): Map<*, *> {
        val newMap = when (map) {
            is HashMap<*, *> -> HashMap<Any, Any>()
            is SortedMap<*, *> -> TreeMap()
            else -> throw IllegalArgumentException("Unsupported map type: ${map::class.java}")
        }
        visited[map] = newMap
        for ((key, value) in map) {
            val newKey = deepCopyHelper(key, visited)
            val newValue = deepCopyHelper(value, visited)
            newMap[newKey as Any] = newValue as Any
        }
        return newMap
    }

    private fun findDeepCloner(clz: Class<*>): DeepCloner {
        return when {
            Enum::class.java.isAssignableFrom(clz) -> IGNORE_CLONER
            Freezable::class.java.isAssignableFrom(clz) -> FreezableCloner(clz)
            isImmutable(clz) -> IGNORE_CLONER
            clz.isArray -> CloneArrayCloner(clz)
            else -> CloneObjectCloner(clz)
        }
    }

    private fun isImmutable(clz: Class<*>): Boolean {
        return immutables.computeIfAbsent(clz) {
            clz.declaredAnnotations.any { it.annotationClass.java == getImmutableAnnotation() } ||
                    generateSequence(clz.superclass) { it.superclass }
                        .any { superClass ->
                            superClass.declaredAnnotations.any {
                                it.annotationClass.java == Immutable::class.java && (it as Immutable).subClass
                            }
                        }
        }
    }

    private fun allFields(c: Class<*>): List<Field> {
        return fieldsCache.computeIfAbsent(c) {
            generateSequence(c) { it.superclass }
                .takeWhile { it != Any::class.java }
                .flatMap { it.declaredFields.asSequence() }
                .toList()
        }
    }

    private fun applyCloningStrategy(
        clones: MutableMap<Any?, Any?>,
        o: Any,
        fieldObject: Any,
        field: Field,
    ): Any? {
        cloningStrategies?.forEach { strategy ->
            return when (strategy.strategyFor(o, field)) {
                CloningStrategy.Strategy.NULL_INSTEAD_OF_CLONE -> null
                CloningStrategy.Strategy.SAME_INSTANCE_INSTEAD_OF_CLONE -> fieldObject
                CloningStrategy.Strategy.IGNORE -> null
            }
        }
        return cloneInternal(fieldObject, clones)
    }

    private fun getImmutableAnnotation() = Immutable::class.java

    companion object {
        private val IGNORE_CLONER: DeepCloner = IgnoreClassCloner()
        private val NULL_CLONER: DeepCloner = NullClassCloner()
    }

    private inner class CloneArrayCloner(
        clz: Class<*>,
    ) : DeepCloner {
        private val primitive = clz.componentType.isPrimitive
        private val immutable = isImmutable(clz.componentType)
        private val componentType = clz.componentType

        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            val length = java.lang.reflect.Array.getLength(o)

            val newInstance = java.lang.reflect.Array.newInstance(componentType, length) as T
            clones?.put(o, newInstance)

            if (primitive || immutable) {
                System.arraycopy(o!!, 0, newInstance!!, 0, length)
            } else {
                for (i in 0 until length) {
                    java.lang.reflect.Array.set(
                        newInstance, i,
                        cloneInternal(java.lang.reflect.Array.get(o, i), clones) ?: java.lang.reflect.Array.get(o, i)
                    )
                }
            }
            return newInstance
        }
    }

    private class FreezableCloner(clz: Class<*>) : DeepCloner {
        private val cloner: DeepCloner = CloneObjectCloner(clz)

        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            if (o is Freezable && o.isFrozen()) {
                return o
            }
            return cloner.deepClone(o, clones)
        }
    }

    private class CloneObjectCloner(clz: Class<*>) : DeepCloner {
        private val fields: Array<Field>
        private val shouldClone: BooleanArray
        private val instantiation = ClonerService().instantiationStrategy.getInstantiateOf(clz)

        init {
            val (fieldList, shouldCloneList) = mutableListOf<Field>() to mutableListOf<Boolean>()
            generateSequence(clz) { it.superclass }
                .takeWhile { it != Any::class.java }
                .forEach { currentClass ->
                    currentClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }.forEach { field ->
                        field.isAccessible = true
                        if (!field.annotations.any { it.annotationClass.java in ClonerService().nullInsteadFieldAnnotations }
                        ) {
                            fieldList.add(field)
                            shouldCloneList.add(true)
                        }
                    }
                }
            fields = fieldList.toTypedArray()
            shouldClone = shouldCloneList.toBooleanArray()
        }

        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            try {
                val newInstance = instantiation.newInstance() as T
                clones?.put(o, newInstance)
                for (i in fields.indices) {
                    val field = fields[i]
                    field.isAccessible = true
                    field[newInstance] = if (shouldClone[i]) applyCloningStrategy(clones!!, o!!, field[o], field) else field[o]
                }
                return newInstance
            } catch (e: IllegalAccessException) {
                throw CloningException(e)
            }
        }

        private fun applyCloningStrategy(
            clones: MutableMap<Any?, Any?>,
            o: Any,
            fieldObject: Any?,
            field: Field,
        ): Any? {
            return ClonerService().applyCloningStrategy(clones, o, fieldObject!!, field)
        }
    }

    private class IgnoreClassCloner : DeepCloner {
        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            throw CloningException("Don't call this directly")
        }
    }

    private class NullClassCloner : DeepCloner {
        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            throw CloningException("Don't call this directly")
        }
    }
}