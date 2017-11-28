package io.codekvast.dashboard.sample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * @author olle.hallin@crisp.se
 */
class SamplePojoTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())

    @Test
    fun should_be_equal() {
        val pojo = SamplePojo(a = 1, b = "b", c = null)
        assertThat(pojo, equalTo(SamplePojo(1, "b", null)))
    }

    @Test
    fun should_serialize_to_json() {
        val src = SamplePojo(1, "b", 2.1)
        val json = objectMapper.writeValueAsString(src)
        val deserialized = objectMapper.readValue(json, SamplePojo::class.java)
        assertThat(deserialized, equalTo(src))
    }
}