package com.home.playground.service.cloning

interface IDeepCloner {
    fun <T> deepClone(o: T, clones: MutableMap<Any?, Any?>?): T
}