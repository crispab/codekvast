package io.codekvast.dashboard.dashboard;

import com.google.gson.Gson;
import io.codekvast.dashboard.bootstrap.CodekvastDashboardSettings;
import io.codekvast.dashboard.dashboard.model.methods.*;
import io.codekvast.javaagent.model.v2.SignatureStatus2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author olle.hallin@crisp.se
 */
public class DashboardApiControllerTest {
    @Mock
    private DashboardService dashboardService;

    private CodekvastDashboardSettings settings = new CodekvastDashboardSettings();

    private MockMvc mockMvc;

    private final Gson gson = new Gson();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DashboardApiController dashboardApiController = new DashboardApiController(dashboardService, settings);
        this.mockMvc = MockMvcBuilders.standaloneSetup(dashboardApiController)
                                      .setMessageConverters(new GsonHttpMessageConverter(), new StringHttpMessageConverter())
                                      .build();
    }

    @Test
    public void should_getMethods_V2() throws Exception {
        // given
        GetMethodsRequest request = GetMethodsRequest.defaults().toBuilder()
                                                     .signature("some signature").build();
        when(dashboardService.getMethods2(request)).thenReturn(GetMethodsResponse2
                                                                   .builder()
                                                                   .methods(emptyList())
                                                                   .build());

        // when
        mockMvc.perform(post("/dashboard/api/v2/methods")
                            .accept(APPLICATION_JSON_UTF8)
                            .contentType(APPLICATION_JSON_UTF8)
                            .content(gson.toJson(request)))
               .andExpect(status().isOk())
               .andExpect(content().contentType(APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.methods").isArray());

    }

    @Test
    public void should_get_methodById() throws Exception {
        Instant now = Instant.now();
        long invokedAtMillis = now.minusSeconds(2500).toEpochMilli();
        long collectedSinceMillis = now.minus(17, ChronoUnit.DAYS).toEpochMilli();
        long collectedToMillis = now.minusSeconds(30).toEpochMilli();

        MethodDescriptor1 methodDescriptor1 = MethodDescriptor1.builder()
                                                               .id(17L)
                                                               .signature("sig")
                                                               .visibility("public")
                                                               .synthetic(false)
                                                               .bridge(false)
                                                               .collectedInEnvironment(
                                                                   EnvironmentDescriptor.builder()
                                                                                        .name("dev")
                                                                                        .collectedSinceMillis(collectedSinceMillis)
                                                                                        .collectedToMillis(collectedToMillis)
                                                                                        .invokedAtMillis(invokedAtMillis)
                                                                                        .tag("tag1")
                                                                                        .tag("tag2")
                                                                                        .build().computeFields())
                                                               .occursInApplication(
                                                                   ApplicationDescriptor.builder()
                                                                                        .name("application")
                                                                                        .version("version")
                                                                                        .startedAtMillis(collectedSinceMillis)
                                                                                        .publishedAtMillis(collectedToMillis)
                                                                                        .invokedAtMillis(invokedAtMillis)
                                                                                        .status(SignatureStatus2.INVOKED)
                                                                                        .build())
                                                               .build().computeFields();

        when(dashboardService.getMethodById(17L)).thenReturn(Optional.of(methodDescriptor1));
        mockMvc.perform(get("/dashboard/api/v1/method/detail/17"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.signature").value("sig"))
               .andExpect(jsonPath("$.visibility").value("public"))
               .andExpect(jsonPath("$.lastInvokedAtMillis").value(invokedAtMillis))
               .andExpect(jsonPath("$.collectedDays").value(16));

        verify(dashboardService).getMethodById(17L);
    }

    @Test
    public void should_get_filterData() throws Exception {
        // given
        when(dashboardService.getMethodsFormData()).thenReturn(GetMethodsFormData.sample());

        mockMvc.perform(get("/dashboard/api/v1/methodsFormData"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(APPLICATION_JSON_UTF8))
               .andExpect(jsonPath("$.applications").isArray())
               .andExpect(jsonPath("$.environments").isArray());
    }
}