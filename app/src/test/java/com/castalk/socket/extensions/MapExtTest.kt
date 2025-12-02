package com.castalk.socket.extensions

import org.junit.Assert.*
import org.junit.Test

class MapExtTest {

    @Test
    fun `null receiver + no pairs returns empty map`() {
        val x: Map<String, Any?>? = null
        val y = x.plusIfAbsent()
        assertTrue(y.isEmpty())
    }

    @Test
    fun `null receiver + only non-null pairs returns copies pairs`() {
        val y = (null as Map<String, Any?>?).plusIfAbsent("a" to 1, "b" to "x")
        assertEquals(linkedMapOf("a" to 1, "b" to "x"), y)
    }

    @Test
    fun `null receiver + some null-valued pairs returns ignore nulls`() {
        val y = (null as Map<String, Any?>?).plusIfAbsent("a" to null, "b" to 2)
        assertEquals(linkedMapOf("b" to 2), y)
        assertFalse(y.containsKey("a"))
    }

    @Test
    fun `empty map receiver + pairs adds only non-null`() {
        val x = emptyMap<String, Any?>()
        val y = x.plusIfAbsent("a" to 1, "b" to null, "c" to true)
        assertEquals(linkedMapOf("a" to 1, "c" to true), y)
    }

    @Test
    fun `non-empty receiver + no pairs returns shallow copy`() {
        val x = mapOf("a" to 1, "b" to null)
        val y = x.plusIfAbsent()
        assertEquals(x, y)
        assertNotSame("Must return a new map instance", x, y)
    }

    @Test
    fun `existing key in X is not overwritten by non-null pair`() {
        val x = mapOf("a" to 1)
        val y = x.plusIfAbsent("a" to 999)
        assertEquals(linkedMapOf("a" to 1), y)
    }

    @Test
    fun `existing key in X with null value still blocks insertion`() {
        val x = mapOf("a" to null)
        val y = x.plusIfAbsent("a" to 42)
        // Key exists (even if value is null) -> do not insert/overwrite
        assertTrue("a should remain present with null", y.containsKey("a"))
        assertNull(y["a"])
        assertEquals(1, y.size)
    }

    @Test
    fun `pair with null value does not insert even if key missing`() {
        val x = mapOf("a" to 1)
        val y = x.plusIfAbsent("b" to null)
        assertEquals(linkedMapOf("a" to 1), y)
        assertFalse(y.containsKey("b"))
    }

    @Test
    fun `order preserved - X entries first then newly added pairs in order`() {
        val x = linkedMapOf("a" to 1, "b" to 2)
        val y = x.plusIfAbsent("c" to 3, "d" to 4)
        assertEquals(listOf("a", "b", "c", "d"), y.keys.toList())
    }

    @Test
    fun `duplicate keys in pairs - only first successful insert counts`() {
        val x = mapOf<String, Any?>()
        val y = x.plusIfAbsent(
            "a" to 1,      // inserts
            "a" to 2,              // ignored (already present)
            "b" to null,           // ignored (null)
            "b" to 3               // inserts (previous null didn't add the key)
        )
        assertEquals(linkedMapOf("a" to 1, "b" to 3), y)
    }

    @Test
    fun `mixed value types are allowed (Any?)`() {
        val x = mapOf("i" to 1)
        val y = x.plusIfAbsent(
            "s" to "str",
            "b" to false,
            "l" to listOf(1, 2),
            "n" to null
        )
        assertEquals(4, y.size)
        assertEquals(1, y["i"])
        assertEquals("str", y["s"])
        assertEquals(false, y["b"])
        assertEquals(listOf(1, 2), y["l"])
        assertFalse(y.containsKey("n"))
    }

    @Test
    fun `adding key already added by earlier pair prevents later pairs`() {
        val x = mapOf("x" to 0)
        val y = x.plusIfAbsent("a" to 1, "a" to 2, "a" to 3)
        assertEquals(linkedMapOf("x" to 0, "a" to 1), y)
    }

    @Test
    fun `later non-null pair can add after an earlier null pair for the same key`() {
        val x = emptyMap<String, Any?>()
        val y = x.plusIfAbsent("k" to null, "k" to "now-non-null")
        assertEquals(linkedMapOf("k" to "now-non-null"), y)
    }

    @Test
    fun `keys list reflects presence even with null value from X`() {
        val x = linkedMapOf("k" to null)
        val y = x.plusIfAbsent("k" to "something")
        assertTrue(y.containsKey("k"))
        assertNull(y["k"])
        assertEquals(listOf("k"), y.keys.toList())
    }
}
