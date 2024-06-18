package com.home.playground.exception

class CloningException : RuntimeException {
    private val serialVersionUID = 4345175312011246867L

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

    constructor(e: Exception) : super(e)
}
