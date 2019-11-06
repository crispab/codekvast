package io.codekvast.backoffice.rules.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Value;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author olle.hallin@crisp.se
 */
class InstantTypeAdapterTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter()).create();

    @Value
    private static class TestObject {
        private final String name;
        private final Instant instant;
    }

    @Test
    void should_serialize_instant_as_string() {
        // given
        Instant now = Instant.now();
        TestObject testObject = new TestObject("name", now);

        // when
        String json = gson.toJson(testObject);

        // then
        assertThat(json, Matchers.containsString(now.toString()));

        assertThat(gson.fromJson(json, TestObject.class), is(testObject));
    }
}
