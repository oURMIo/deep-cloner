package com.home.playground

import com.home.playground.dto.PrivateConstructorDto
import com.home.playground.dto.StudentDto
import com.home.playground.util.CopyUtils
import java.util.SortedMap
import java.util.TreeSet
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
        assertEquals(deepCopy.size, 3)
    }

    @Test
    fun deepCopyDto() {
        val sasha = StudentDto("Sasha", 20, mutableListOf())
        val masha = StudentDto("Masha", 20, mutableListOf(sasha))
        sasha.friends.add(masha)

        val deepCopy = CopyUtils.deepCopy(sasha)
        assertEquals(deepCopy, deepCopy.friends[0].friends[0])
    }

    @Test
    fun deepCopyDtoPlus() {
        val sasha = StudentDto("Sasha", 20, mutableListOf())
        val masha = StudentDto("Masha", 20, mutableListOf(sasha))
        sasha.friends.add(masha)
        assertEquals(1, sasha.friends.size)

        val clonedSasha = CopyUtils.deepCopy(sasha)
        assertEquals(clonedSasha, clonedSasha.friends[0].friends[0])
        assertEquals(1, clonedSasha.friends.size)

        clonedSasha.friends.add(StudentDto("Vil", 20, mutableListOf()))
        assertEquals(2, clonedSasha.friends.size)
        assertEquals(1, sasha.friends.size)
    }

    @Test
    fun deepCopyTreeSet() {
        val sortedSet: TreeSet<Int> = sortedSetOf(1, 2, 3, 4, 5)

        assertAll(
            { assertEquals(1, sortedSet.first()) },
            { assertEquals(2, sortedSet.toList()[1]) },
            { assertEquals(3, sortedSet.toList()[2]) },
        )

        val deepCopy: TreeSet<Int> = CopyUtils.deepCopy(sortedSet)
        assertAll(
            { assertEquals(1, deepCopy.first()) },
            { assertEquals(2, deepCopy.toList()[1]) },
            { assertEquals(3, deepCopy.toList()[2]) },
        )
    }

    @Test
    fun deepCopySortedMap() {
        val sortedMap = sortedMapOf(3 to "c", 1 to "a", 2 to "b")
        testSortedImpl(sortedMap)
        val deepCopy = CopyUtils.deepCopy(sortedMap)
        testSortedImpl(deepCopy)
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
        assertEquals(deepCopy.x, null)
    }

    @Test
    fun deepCopyPrivateConstructor() {
        val x = PrivateConstructorDto.getInstance()

        assertEquals(x.value, 1408)

        val deepCopy = CopyUtils.deepCopy(x)
        assertEquals(deepCopy.value, 1408)
    }
}