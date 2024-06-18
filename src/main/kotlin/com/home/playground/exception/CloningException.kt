package com.home.playground.exception

import java.io.Serial

class CloningException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

    constructor(e: Exception) : super(e)

    companion object {
        @Serial
        private const val serialVersionUID = 31415L
    }
}
