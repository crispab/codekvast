package io.codekvast.common.customer.impl;

import io.codekvast.common.customer.CustomerData;
import io.codekvast.common.customer.CustomerService;
import io.codekvast.common.customer.LicenseViolationException;
import io.codekvast.common.messaging.SlackService;
import io.codekvast.common.metrics.CommonMetricsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class CustomerServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private SlackService slackService;

    @Mock
    private CommonMetricsService metricsService;

    private CustomerService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        service = new CustomerServiceImpl(jdbcTemplate, slackService, metricsService);

        Map<String, Object> map = new HashMap<>();
        map.put("id", 1L);
        map.put("createdAt", new Timestamp(System.currentTimeMillis()));
        map.put("name", "name");
        map.put("plan", "test");
        map.put("source", "source");

        when(jdbcTemplate.queryForMap(anyString(), any())).thenReturn(map);
    }

    @Test
    public void should_return_sensible_CustomerData() {
        CustomerData data = service.getCustomerDataByLicenseKey("key");
        assertThat(data.getCustomerId(), is(1L));
        assertThat(data.getPricePlan().getName(), is("TEST"));
    }

    @Test(expected = LicenseViolationException.class)
    public void should_reject_too_many_methods() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("test");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(50_000L);

        service.assertDatabaseSize(1L);
    }

    @Test(expected = LicenseViolationException.class)
    public void should_reject_too_big_codeBasePublication() {
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(1L))).thenReturn("test");
        CustomerData customerData = service.getCustomerDataByLicenseKey("");
        service.assertPublicationSize(customerData, 100_000);
    }
}