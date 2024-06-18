package com.home.playground.service.cloning

interface DeepCloner {
    fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T
}