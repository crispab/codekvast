package io.codekvast.common.customer.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.messaging.EventService;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.messaging.model.LicenseViolationEvent;
import io.codekvast.common.metrics.CommonMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CustomerServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SlackService slackService;

    @Mock
    private CommonMetricsService metricsService;

    @Mock
    private EventService eventService;

    private CustomerService service;

    @BeforeEach
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        service = new CustomerServiceImpl(jdbcTemplate, slackService, metricsService, eventService);

        Map<String, Object> map = new HashMap<>();
        map.put("id", 1L);
        map.put("createdAt", new Timestamp(System.currentTimeMillis()));
        map.put("name", "name");
        map.put("plan", "test");
        map.put("source", "source");

        when(jdbcTemplate.queryForMap(startsWith("SELECT c.id, c.name, c.source"), any())).thenReturn(map);
    }

    @Test
    public void should_return_sensible_CustomerData() {
        // Given

        // When
        CustomerData data = service.getCustomerDataByLicenseKey("key");

        // Then
        assertThat(data.getCustomerId(), is(1L));
        assertThat(data.getPricePlan().getName(), is("TEST"));

        verifyNoInteractions(eventService);
    }

    @Test
    public void should_reject_too_many_methods() {
        // Given
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1) FROM methods WHERE"), eq(Long.class), eq(1L))).thenReturn(50_000L);

        // When
        LicenseViolationException exception = assertThrows(LicenseViolationException.class,
                                                           () -> service.assertDatabaseSize(1L));

        // Then
        assertThat(exception.getMessage(), containsString("Too many methods"));
        assertThat(exception.getMessage(), containsString("50000"));

        verify(eventService).send(any(LicenseViolationEvent.class));
    }

    @Test
    public void should_reject_too_big_codeBasePublication() {
        CustomerData customerData = service.getCustomerDataByLicenseKey("");

        LicenseViolationException exception = assertThrows(LicenseViolationException.class,
                     () -> service.assertPublicationSize(customerData, 100_000));
        assertThat(exception.getMessage(), containsString("100000"));

        verify(eventService).send(any(LicenseViolationEvent.class));
    }
}
