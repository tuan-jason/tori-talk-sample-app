package com.torilab.socket.di

import com.torilab.android.common.JsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ParserModuleTest {

    private val module = ParserModule()

    data class Sample(val value: String)

    @Test
    fun `provideMoshi supplies kotlin adapter`() {
        val moshi = module.provideMoshi()
        val json = moshi.adapter(Sample::class.java).toJson(Sample("test"))

        assertEquals("""{"value":"test"}""", json)
    }

    @Test
    fun `provideJsonHelper reuses provided moshi`() {
        val moshi = module.provideMoshi()
        val helper: JsonHelper = module.provideJsonHelper(moshi)

        val json = helper.toJson(Sample("value"), Sample::class.java)
        val restored = helper.toObject(json, Sample::class.java)

        assertNotNull(restored)
        assertEquals("value", restored?.value)
    }
}
