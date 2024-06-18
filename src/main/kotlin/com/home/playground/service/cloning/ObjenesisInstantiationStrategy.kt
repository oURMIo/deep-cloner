package com.home.playground.service.cloning

import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator

class ObjenesisInstantiationStrategy : InstantiationStrategy {
    private val objenesis = ObjenesisStd()

    override fun <T> newInstance(c: Class<T>): T {
        return objenesis.newInstance(c)
    }

    override fun <T> getInstantiateOf(c: Class<T>): ObjectInstantiator<T> {
        return objenesis.getInstantiatorOf(c)
    }

    companion object {
        private val instance = ObjenesisInstantiationStrategy()

        fun getInstance(): ObjenesisInstantiationStrategy {
            return instance
        }
    }
}
