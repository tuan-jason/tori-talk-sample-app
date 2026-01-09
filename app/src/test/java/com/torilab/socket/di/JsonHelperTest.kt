package com.torilab.socket.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class JsonHelperTest {

    private lateinit var jsonHelper: JsonHelper
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        jsonHelper = JsonHelper(moshi)
    }

    @Test
    fun `toJson converts object to JSON string`() {
        val testData = TestData("test", 123)
        val json = jsonHelper.toJson(testData, TestData::class.java)
        
        assertNotNull(json)
        assert(json.contains("\"name\":\"test\""))
        assert(json.contains("\"value\":123"))
    }

    @Test
    fun `toJson returns empty string for null object`() {
        val json = jsonHelper.toJson(null, TestData::class.java)
        assertEquals("", json)
    }

    @Test
    fun `toObject converts JSON string to object`() {
        val json = """{"name":"test","value":123}"""
        val obj = jsonHelper.toObject(json, TestData::class.java)
        
        assertNotNull(obj)
        assertEquals("test", obj?.name)
        assertEquals(123, obj?.value)
    }

    @Test
    fun `toObject returns null for null JSON string`() {
        val obj = jsonHelper.toObject(null, TestData::class.java)
        assertNull(obj)
    }

    @Test
    fun `toObject returns null for empty JSON string`() {
        val obj = jsonHelper.toObject("", TestData::class.java)
        assertNull(obj)
    }

    @Test
    fun `toArray converts JSON array string to list`() {
        val json = """[{"name":"test1","value":1},{"name":"test2","value":2}]"""
        val list = jsonHelper.toArray(json, TestData::class.java)
        
        assertNotNull(list)
        assertEquals(2, list?.size)
        assertEquals("test1", list?.get(0)?.name)
        assertEquals(2, list?.get(1)?.value)
    }

    @Test
    fun `toArray returns null for null JSON string`() {
        val list = jsonHelper.toArray(null, TestData::class.java)
        assertNull(list)
    }

    @Test
    fun `toArray returns null for empty JSON string`() {
        val list = jsonHelper.toArray("", TestData::class.java)
        assertNull(list)
    }

    data class TestData(val name: String, val value: Int)
}
