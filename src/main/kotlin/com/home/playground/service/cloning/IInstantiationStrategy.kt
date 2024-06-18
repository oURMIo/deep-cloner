package com.home.playground.service.cloning

import org.objenesis.instantiator.ObjectInstantiator

interface IInstantiationStrategy {
    fun <T> newInstance(c: Class<T>): T
    fun <T> getInstantiateOf(c: Class<T>): ObjectInstantiator<T>
}