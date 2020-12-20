package io.codekvast.common.lib.logback

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HostnamePropertyDefinerTest {

    @Test
    fun should_get_name_of_localhost() {
        assertNotNull(HostnamePropertyDefiner().propertyValue)
    }
}