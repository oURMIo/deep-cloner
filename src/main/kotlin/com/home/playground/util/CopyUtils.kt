package com.home.playground.util

import com.home.playground.service.ClonerService

object CopyUtils {
    private val clonerService = ClonerService()

    fun <T> deepCopy(obj: T?): T? {
        if (obj == null ||
            obj is Enum<*> ||
            obj is String ||
            obj is Number ||
            obj is Boolean ||
            obj is Char
        ) {
            return obj
        }
        return try {
            viewAs(obj)
        } catch (e: Exception) {
            throw RuntimeException("Failed to deep copy object", e)

        }
    }

    fun <T> deepCopy(collection: Collection<T>?): MutableList<T?>? {
        return collection?.map { deepCopy(it) }?.toMutableList()
    }

    private fun <T> viewAs(o: T): T {
        val clazz = o!!::class.java
        val newInstance = clonerService.fastCloneOrNewInstance(clazz) as T
        clonerService.copyPropertiesOfInheritedClass(o, newInstance)
        return newInstance
    }
}
