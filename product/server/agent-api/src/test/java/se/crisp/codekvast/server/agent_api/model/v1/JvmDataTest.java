


package se.crisp.codekvast.server.agent_api.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class JvmDataTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Validator validator;

    @Before
    public void beforeTest() throws Exception {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        this.validator = factory.getValidator();
    }

    @Test
    public void jvmDataShouldBeValid() {
        // given
        JvmData data1 = getJvmData("tag1, tag2, tag3");

        // when
        Set<ConstraintViolation<JvmData>> violations = validator.validate(data1);

        // then
        assertThat(violations, empty());

    }

    @Test
    public void jvmDataWithTagsShouldBeJsonSerializable() throws IOException {
        // given
        JvmData data1 = getJvmData("tag1, tag2, tag3");

        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmData data2 = objectMapper.readValue(json, JvmData.class);

        // then
        assertThat(data1, is(data2));
    }

    @Test
    public void jvmDataWithoutTagsShouldBeJsonSerializable() throws IOException {
        // given
        JvmData data1 = getJvmData("");

        // when
        String json = objectMapper.writeValueAsString(data1);
        JvmData data2 = objectMapper.readValue(json, JvmData.class);

        // then
        assertThat(data1, is(data2));
    }

    private JvmData getJvmData(String tags) {
        return JvmData.builder()
                      .agentComputerId("agentComputerId")
                      .agentHostName("agentHostName")
                      .agentUploadIntervalSeconds(300)
                      .appName("appName")
                      .appVersion("appVersion")
                      .codekvastVcsId("codekvastVcsId")
                      .codekvastVersion("codekvastVersion")
                      .collectorComputerId("collectorComputerId")
                      .collectorHostName("collectorHostName")
                      .collectorResolutionSeconds(600)
                      .dumpedAtMillis(2000L)
                      .jvmUuid(UUID.randomUUID().toString())
                      .methodVisibility("methodVisibility")
                      .startedAtMillis(1000L)
                      .tags(tags)
                      .build();
    }

}
