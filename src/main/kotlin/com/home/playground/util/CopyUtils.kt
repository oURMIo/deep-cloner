package com.home.playground.util

import com.home.playground.service.ClonerService
import java.lang.reflect.Array
import java.util.IdentityHashMap
import java.util.SortedMap
import java.util.TreeMap
import java.util.TreeSet

object CopyUtils {
    private val clonerService = ClonerService()

    fun <T> deepCopy(obj: T): T {
        return try {
            deepCopyHelper(obj, IdentityHashMap())
        } catch (e: Exception) {
            throw RuntimeException("Deep copy failed", e)
        }
    }

    private fun <T> deepCopyHelper(obj: T, visited: MutableMap<Any, Any>): T {
        if (obj == null) {
            return obj
        }

        if (isPrimitiveOrWrapper(obj!!::class.java)) {
            return obj
        }

        if (visited.containsKey(obj)) {
            return visited[obj] as T
        }

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
            viewAs(obj)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deep copy object", e)
        }
    }

    private fun <T> viewAs(o: T): T {
        val clazz = o!!::class.java
        val newInstance = clonerService.fastCloneOrNewInstance(clazz) as T
        clonerService.copyPropertiesOfInheritedClass(o, newInstance)
        return newInstance
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
        val length = Array.getLength(array)
        val componentType = array.javaClass.componentType
        val newArray = Array.newInstance(componentType, length)
        visited[array] = newArray
        for (i in 0 until length) {
            Array.set(newArray, i, deepCopyHelper(Array.get(array, i), visited))
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
}
