package com.home.playground

import com.home.playground.dto.ManDto
import com.home.playground.util.CopyUtils
import kotlin.random.Random

fun main() {
    val orig = ManDto("Bob", 21, mutableListOf("Maker Beer", "Fight club"))
    val copy = CopyUtils.deepCopy(orig)
    val isEquals = orig == copy
    println("Original equals copy is '$isEquals', after check will change orig")

    orig.name = "Viktor"
    orig.age = Random.nextInt(10, 100)
    orig.favoriteBooks = mutableListOf("Woe from mind", "One Hundred Years of Solitude.")
    println("orig:$orig")
    println("copy:$copy")
}