package com.home.playground.service.cloning

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
@Retention(RUNTIME)
annotation class Immutable(
    val subClass: Boolean = false,
)

