package com.home.playground

import com.home.playground.dto.StudentDto
import com.home.playground.util.CopyUtils
import java.util.SortedMap
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
class CopyUtilsTest {

    @Test
    fun deepCopyDouble() {
        val orig = 3.14159
        val copy = CopyUtils.deepCopy(orig)
        assertEquals(orig, copy)
    }

    @Test
    fun deepCopyList() {
        val deepCopy = CopyUtils.deepCopy(listOf(1, 2, 3))
        assertEquals(deepCopy?.size, 3)
    }

    @Test
    fun deepCopyDto() {
        val sasha = StudentDto("Sasha", 20, mutableListOf())
        val masha = StudentDto("Masha", 20, mutableListOf(sasha))
        sasha.friends.add(masha)

        val deepCopy = CopyUtils.deepCopy(sasha)
        deepCopy?.let {
            assertEquals(deepCopy, deepCopy.friends[0].friends[0])
        }
    }

    @Test
    fun deepCopyTreeSet() {
        val sortedSet = sortedSetOf(1, 2, 3, 4, 5)

        assertAll(
            { assertEquals(1, sortedSet.first()) },
            { assertEquals(2, sortedSet.toList()[1]) },
            { assertEquals(3, sortedSet.toList()[2]) },
        )

        val deepCopy = CopyUtils.deepCopy(sortedSet)
        assertAll(
            { deepCopy?.let { assertEquals(1, it.first()) } },
            { deepCopy?.let { assertEquals(2, it.toList()[1]) } },
            { deepCopy?.let { assertEquals(3, it.toList()[2]) } },
        )
    }

    @Test
    fun deepCopySortedMap() {
        val sortedMap = sortedMapOf(3 to "c", 1 to "a", 2 to "b")
        testSortedImpl(sortedMap)
        val deepCopy = CopyUtils.deepCopy(sortedMap)
        deepCopy?.let { testSortedImpl(it) }
    }

    private fun testSortedImpl(sortedMap: SortedMap<Int, String>) {
        assertAll(
            { assertEquals("a", sortedMap.values.first()) },
            { assertEquals("b", sortedMap.values.elementAt(1)) },
            { assertEquals("c", sortedMap.values.last()) },
        )
    }

    @Serializable
    class X(var x: String? = null)

    @Test
    fun deepCopyOnNull() {
        val x = X()

        assertEquals(x.x, null)

        val deepCopy = CopyUtils.deepCopy(x)
        assertEquals(deepCopy?.x, null)
    }

    @Serializable
    class PrivateConstructorClass private constructor(val value: Int) {
        companion object {
            fun getInstance(): PrivateConstructorClass {
                return PrivateConstructorClass(42)
            }
        }
    }

    @Test
    fun deepCopyPrivateConstructor() {
        val x = PrivateConstructorClass.getInstance()

        assertEquals(x.value, 42)

        val deepCopy = CopyUtils.deepCopy(x)
        assertEquals(deepCopy?.value, 42)
    }
}