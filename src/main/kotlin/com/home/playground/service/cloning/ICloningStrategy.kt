package com.home.playground.service.cloning

import java.lang.reflect.Field

interface ICloningStrategy {
    enum class Strategy {
        NULL_INSTEAD_OF_CLONE,
        SAME_INSTANCE_INSTEAD_OF_CLONE,
        IGNORE
    }

    fun strategyFor(toBeCloned: Any, field: Field): Strategy
}