package com.home.playground.dto

data class StudentDto(
    var name: String,
    var age: Int,
    var friends: MutableList<StudentDto>,
)
