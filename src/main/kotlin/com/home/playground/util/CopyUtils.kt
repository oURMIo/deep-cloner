package com.home.playground.util

import com.home.playground.service.ClonerService

object CopyUtils {
    private val clonerService = ClonerService()

    fun <T> deepCopy(obj: T): T {
        return try {
            clonerService.clone(obj)
        } catch (e: Exception) {
            throw RuntimeException("Deep copy failed", e)
        }
    }
}
