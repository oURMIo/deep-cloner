package com.home.playground.service

import com.home.playground.exception.CloningException
import com.home.playground.service.cloning.ICloningStrategy
import com.home.playground.service.cloning.IDeepCloner
import com.home.playground.service.cloning.IFreezable
import com.home.playground.service.cloning.IInstantiationStrategy
import com.home.playground.service.cloning.Immutable
import com.home.playground.service.cloning.ObjenesisInstantiationStrategy
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

@Suppress("UNCHECKED_CAST")
class ClonerService {
    private val instantiationStrategy: IInstantiationStrategy = ObjenesisInstantiationStrategy.getInstance()
    private val nullInsteadFieldAnnotations = mutableSetOf<Class<*>>()
    private val fieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()
    private val cloningStrategies: MutableList<ICloningStrategy>? = null

    private val cloners = ConcurrentHashMap<Class<*>, IDeepCloner>()
    private val immutables = ConcurrentHashMap<Class<*>, Boolean>()

    fun <T> fastCloneOrNewInstance(c: Class<T>): T {
        return instantiationStrategy.newInstance(c)
    }

    fun <T, E : T> copyPropertiesOfInheritedClass(src: T?, est: E?) {
        requireNotNull(src) { "src can't be null" }
        requireNotNull(est) { "est can't be null" }
        val srcClz = src.javaClass
        val estClz = est.javaClass

        when {
            srcClz.isArray -> {
                if (!estClz.isArray) throw IllegalArgumentException("Can't copy from array to non-array class $estClz")
                val length = java.lang.reflect.Array.getLength(src)
                for (i in 0 until length) {
                    val v = java.lang.reflect.Array.get(src, i)
                    java.lang.reflect.Array.set(est, i, v)
                }
            }

            src is List<*> && est is MutableList<*> -> {
                (est as MutableList<Any?>).apply {
                    clear()
                    addAll(src as List<Any?>)
                }
            }

            src is Map<*, *> && est is MutableMap<*, *> -> {
                (est as MutableMap<Any?, Any?>).apply {
                    clear()
                    putAll(src as Map<Any?, Any?>)
                }
            }

            else -> {
                val fields = allFields(srcClz)
                val estFields = allFields(estClz)
                fields.filterNot { Modifier.isStatic(it.modifiers) }.forEach { field ->
                    try {
                        field.isAccessible = true
                        val fieldObject = field[src]
                        if (field in estFields) {
                            field[est] = fieldObject
                        }
                    } catch (e: IllegalArgumentException) {
                        throw CloningException(e)
                    } catch (e: IllegalAccessException) {
                        throw CloningException(e)
                    }
                }
            }
        }
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

    private fun findDeepCloner(clz: Class<*>): IDeepCloner {
        return when {
            Enum::class.java.isAssignableFrom(clz) -> IGNORE_CLONER
            IFreezable::class.java.isAssignableFrom(clz) -> IFreezableCloner(clz)
            isImmutable(clz) -> IGNORE_CLONER
            clz.isArray -> CloneArrayCloner(clz)
            else -> CloneObjectCloner(clz)
        }
    }

    private fun getImmutableAnnotation() = Immutable::class.java

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
                ICloningStrategy.Strategy.NULL_INSTEAD_OF_CLONE -> null
                ICloningStrategy.Strategy.SAME_INSTANCE_INSTEAD_OF_CLONE -> fieldObject
                ICloningStrategy.Strategy.IGNORE -> null
            }
        }
        return cloneInternal(fieldObject, clones)
    }

    companion object {
        private val IGNORE_CLONER: IDeepCloner = IgnoreClassCloner()
        private val NULL_CLONER: IDeepCloner = NullClassCloner()
    }

    private inner class CloneArrayCloner(
        clz: Class<*>,
    ) : IDeepCloner {
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

    private class IFreezableCloner(clz: Class<*>) : IDeepCloner {
        private val cloner: IDeepCloner = CloneObjectCloner(clz)

        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            if (o is IFreezable && o.isFrozen()) {
                return o
            }
            return cloner.deepClone(o, clones)
        }
    }

    private class CloneObjectCloner(clz: Class<*>) : IDeepCloner {
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

    private class IgnoreClassCloner : IDeepCloner {
        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            throw CloningException("Don't call this directly")
        }
    }

    private class NullClassCloner : IDeepCloner {
        override fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T {
            throw CloningException("Don't call this directly")
        }
    }
}
