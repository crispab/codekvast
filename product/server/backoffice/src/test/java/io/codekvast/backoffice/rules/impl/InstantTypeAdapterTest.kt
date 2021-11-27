package io.codekvast.backoffice.rules.impl

import com.google.gson.GsonBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

/** @author olle.hallin@crisp.se
 */
internal class InstantTypeAdapterTest {
    private val gson = GsonBuilder().registerTypeAdapter(Instant::class.java, InstantTypeAdapter()).create()

    @Test
    fun should_serialize_instant_as_string() {
        // given
        val now = Instant.now()
        val testObject = TestObject("name", now)

        // when
        val json = gson.toJson(testObject)

        // then
        assertThat(json, containsString(now.toString()))
        assertEquals(expected = testObject, actual = gson.fromJson(json, TestObject::class.java))
    }

    @Test
    fun should_serialize_null_instant_as_null() {
        // given
        val testObject = TestObject("name", null)

        // when
        val json = gson.toJson(testObject)

        // then
        assertThat(json, not(containsString(""""instant"""")))
        assertEquals(expected = testObject, actual = gson.fromJson(json, TestObject::class.java))
    }


    data class TestObject(val name: String, val instant: Instant?)
}
