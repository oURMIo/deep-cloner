package com.home.playground.dto

data class PrivateConstructorDto private constructor(var value: Int) {
    companion object {
        fun getInstance(): PrivateConstructorDto {
            return PrivateConstructorDto(1408)
        }
    }
}
