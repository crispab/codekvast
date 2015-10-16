package se.crisp.codekvast.shared.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.config.CollectorConfigFactory;
import se.crisp.codekvast.shared.model.Jvm;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author olle.hallin@crisp.se
 */
@RunWith(Parameterized.class)
public class JsonSerializationTest {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {

        return Arrays.asList(new Object[][]{
                {CollectorConfig.class.getSimpleName(), CollectorConfigFactory.createSampleCollectorConfig()},

                {Jvm.class.getSimpleName(), Jvm.builder()
                                               .collectorConfig(CollectorConfigFactory.createSampleCollectorConfig())
                                               .collectorVcsId("collectorVcsId")
                                               .collectorVersion("collectorVersion")
                                               .computerId("computerId")
                                               .hostName("hostName")
                                               .jvmUuid(UUID.randomUUID().toString())
                                               .startedAtMillis(System.currentTimeMillis())
                        .build()},
                });
    }

    @SuppressWarnings("unused")
    private final String name;
    private final Object object;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSerializationTest(String name, Object object) {
        this.name = name;
        this.object = object;
    }

    @Test
    public void testSerializeDeserialize() throws Exception {
        String serialized = objectMapper.writeValueAsString(object);
        Object deserialized = objectMapper.readValue(serialized, object.getClass());
        assertThat(deserialized, is(object));
    }
}
