package io.codekvast.intake.agent.controller

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.*
import io.codekvast.intake.model.LicenseViolationException
import io.codekvast.intake.agent.service.AgentService
import io.codekvast.javaagent.model.Endpoints
import io.codekvast.javaagent.model.v1.rest.GetConfigRequest1
import io.codekvast.javaagent.model.v1.rest.GetConfigResponse1
import io.codekvast.javaagent.model.v2.GetConfigRequest2
import io.codekvast.javaagent.model.v2.GetConfigResponse2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

class AgentControllerTest {
    private lateinit var mockMvc: MockMvc
    private val gson = Gson()
    private val agentService: AgentService = mock()

    @BeforeEach
    fun setup() {
        val intakeController = AgentController(agentService)

        mockMvc = MockMvcBuilders.standaloneSetup(intakeController)
            .setMessageConverters(GsonHttpMessageConverter(), StringHttpMessageConverter())
            .addFilters<StandaloneMockMvcBuilder>(LogAndSuppressRemoteClosedConnectionException())
            .build()
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_reject_invalid_method() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(Endpoints.Agent.V2_POLL_CONFIG)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed)
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_reject_invalid_media_type() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG)
                    .contentType(MediaType.TEXT_PLAIN)
            )
            .andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType)
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_reject_invalid_json() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG).content("invalid json")
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_reject_invalid_request() {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG)
                    .content(
                        gson.toJson(
                            GetConfigRequest2.sample().toBuilder().appName("").build()
                        )
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_reject_invalid_licenseKey() {
        whenever(agentService.getConfig2(any()))
            .thenThrow(LicenseViolationException("foobar"))

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG)
                    .content(gson.toJson(GetConfigRequest2.sample()))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig1_should_accept_valid_request_with_accept_application_json_1() {
        whenever(agentService.getConfig1(any()))
            .thenReturn(
                GetConfigResponse1.sample().toBuilder().codeBasePublisherName("foobar").build()
            )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V1_POLL_CONFIG)
                    .content(gson.toJson(GetConfigRequest1.sample()))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.codeBasePublisherName").value("foobar"))
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_accept_valid_request_with_accept_application_json_2() {
        whenever(agentService.getConfig2(any()))
            .thenReturn(
                GetConfigResponse2.sample().toBuilder().codeBasePublisherName("foobar").build()
            )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG)
                    .content(gson.toJson(GetConfigRequest2.sample()))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.codeBasePublisherName").value("foobar"))
    }

    @Test
    @Throws(Exception::class)
    fun pollConfig2_should_accept_valid_request_with_accept_application_json_utf8() {
        whenever(agentService.getConfig2(any()))
            .thenReturn(
                GetConfigResponse2.sample().toBuilder().codeBasePublisherName("foobar").build()
            )
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(Endpoints.Agent.V2_POLL_CONFIG)
                    .content(gson.toJson(GetConfigRequest2.sample()))
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.codeBasePublisherName").value("foobar"))
    }

    @Test
    @Throws(Exception::class)
    fun should_accept_upload_codebase_publication2_when_valid_license() {
        assertUploadPublication(
            io.codekvast.intake.model.PublicationType.CODEBASE,
            Endpoints.Agent.V2_UPLOAD_CODEBASE
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_accept_upload_codebase_publication3_when_valid_license() {
        assertUploadPublication(
            io.codekvast.intake.model.PublicationType.CODEBASE,
            Endpoints.Agent.V3_UPLOAD_CODEBASE
        )
    }

    @Test
    @Throws(Exception::class)
    fun should_accept_upload_invocation_data_publication2_when_valid_license() {
        assertUploadPublication(
            io.codekvast.intake.model.PublicationType.INVOCATIONS,
            Endpoints.Agent.V2_UPLOAD_INVOCATION_DATA
        )
    }

    @Throws(Exception::class)
    private fun assertUploadPublication(
        publicationType: io.codekvast.intake.model.PublicationType,
        endpoint: String
    ) {
        val licenseKey = "licenseKey"
        val fingerprint = "fingerprint"
        val publicationSize = 10000
        val originalFilename = String.format("codekvast-%s-9128371293719273.ser", publicationType)
        val multipartFile = MockMultipartFile(
            Endpoints.Agent.PARAM_PUBLICATION_FILE,
            originalFilename,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "PublicationContent-$publicationType".toByteArray()
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart(endpoint)
                    .file(multipartFile)
                    .param(Endpoints.Agent.PARAM_LICENSE_KEY, licenseKey)
                    .param(Endpoints.Agent.PARAM_FINGERPRINT, fingerprint)
                    .param(Endpoints.Agent.PARAM_PUBLICATION_SIZE, publicationSize.toString() + "")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("OK"))
        verify(agentService)
            .savePublication(
                eq(publicationType),
                eq(licenseKey),
                eq(fingerprint),
                eq(publicationSize),
                any()
            )
    }
}