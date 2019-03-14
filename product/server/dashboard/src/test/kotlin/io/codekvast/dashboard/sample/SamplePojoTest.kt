package io.codekvast.dashboard.sample

import com.google.gson.Gson
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * @author olle.hallin@crisp.se
 */
class SamplePojoTest {

    private val gson = Gson()

    @Test
    fun should_be_equal() {
        val pojo = SamplePojo(a = 1, b = "b", c = null)
        assertThat(pojo, equalTo(SamplePojo(1, "b", null)))
    }

    @Test
    fun should_serialize_to_json() {
        val src = SamplePojo(1, "b", 2.1)
        val json = gson.toJson(src)
        val deserialized = gson.fromJson(json, SamplePojo::class.java)
        assertThat(deserialized, equalTo(src))
    }
}