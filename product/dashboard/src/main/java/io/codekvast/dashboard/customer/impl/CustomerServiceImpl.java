/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.dashboard.customer.impl;

import io.codekvast.dashboard.customer.*;
import io.codekvast.dashboard.messaging.SlackService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * @author olle.hallin@crisp.se
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final JdbcTemplate jdbcTemplate;
    private final SlackService slackService;

    @Override
    @Transactional(readOnly = true)
    public CustomerData getCustomerDataByLicenseKey(@NonNull String licenseKey) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("c.licenseKey = ?", licenseKey);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid license key: '" + licenseKey + "'");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerData getCustomerDataByCustomerId(long customerId) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("c.id = ?", customerId);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid customerId: " + customerId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerData getCustomerDataByExternalId(@NonNull String externalId) throws AuthenticationCredentialsNotFoundException {
        try {
            return getCustomerData("c.externalId = ?", externalId);
        } catch (DataAccessException e) {
            throw new AuthenticationCredentialsNotFoundException("Invalid externalId: " + externalId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertPublicationSize(String licenseKey, int publicationSize) throws LicenseViolationException {
        doAssertNumberOfMethods(getCustomerDataByLicenseKey(licenseKey), publicationSize);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertDatabaseSize(long customerId) throws LicenseViolationException {
        CustomerData customerData = getCustomerDataByCustomerId(customerId);
        long numberOfMethods = countMethods(customerId);

        doAssertNumberOfMethods(customerData, numberOfMethods);
    }

    @Override
    @Transactional(readOnly = true)
    public int countMethods(long customerId) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM methods WHERE customerId = ?", Long.class, customerId);
        return Math.toIntExact(count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerData registerAgentDataPublication(CustomerData customerData, Instant publishedAt) {
        final CustomerData result;

        if (customerData.getPricePlan().getMaxCollectionPeriodDays() > 0
            && customerData.getTrialPeriodEndsAt() == null) {
            result = startTrialPeriod(customerData, publishedAt);
        } else if (customerData.getCollectionStartedAt() == null) {
            result = recordCollectionStarted(customerData, publishedAt);
        } else {
            result = customerData;
        }
        return result;
    }

    private CustomerData startTrialPeriod(CustomerData customerData, Instant instant) {
        CustomerData result = customerData.toBuilder()
                                          .collectionStartedAt(instant)
                                          .trialPeriodEndsAt(instant.plus(customerData.getPricePlan().getMaxCollectionPeriodDays(), DAYS))
                                          .build();

        int updated = jdbcTemplate.update("UPDATE customers SET updatedAt = ?, collectionStartedAt = ?, trialPeriodEndsAt = ? " +
                                              "WHERE id = ? ",
                                          Timestamp.from(Instant.now()),
                                          Timestamp.from(result.getCollectionStartedAt()),
                                          Timestamp.from(result.getTrialPeriodEndsAt()),
                                          customerData.getCustomerId());

        if (updated <= 0) {
            logger.warn("Failed to start trial period for {}", result);
        } else {
            slackService.sendNotification(
                String.format("Trial period started for `%s`, ends at %s", result, result.getTrialPeriodEndsAt()),

                SlackService.Channel.BUSINESS_EVENTS);
            logger.info("Started trial period for {}", result);
        }
        return result;
    }

    private CustomerData recordCollectionStarted(CustomerData customerData, Instant instant) {
        CustomerData result = customerData.toBuilder().collectionStartedAt(instant).build();

        int updated = jdbcTemplate.update("UPDATE customers SET updatedAt = ?, collectionStartedAt = ? " +
                                              "WHERE id = ? ",
                                          Timestamp.from(Instant.now()),
                                          Timestamp.from(result.getCollectionStartedAt()),
                                          customerData.getCustomerId());

        if (updated <= 0) {
            logger.warn("Failed to record collection started for {}", result);
        } else {
            logger.info("Collection started for {}", result);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerLogin(LoginRequest request) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        int updated = jdbcTemplate
            .update("UPDATE users SET lastLoginAt = ?, lastActivityAt = ?, lastLoginSource = ?, numberOfLogins = numberOfLogins + 1 " +
                        "WHERE customerId = ? AND email = ?",
                    now, now, request.getSource(), request.getCustomerId(), request.getEmail());
        if (updated > 0) {
            logger.debug("Updated user {}", request);
        } else {
            jdbcTemplate.update(
                "INSERT INTO users(customerId, email, firstLoginAt, lastLoginAt, lastActivityAt, lastLoginSource, numberOfLogins) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)",
                request.getCustomerId(), request.getEmail(), now, now, now, request.getSource(), 1);
            logger.debug("Added user {}", request);
        }
        logger.info("Logged in {}", request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addCustomer(AddCustomerRequest request) {
        String licenseKey = UUID.randomUUID().toString().replaceAll("[-_]", "").toUpperCase();

        jdbcTemplate.update("INSERT INTO customers(source, externalId, name, licenseKey, plan) VALUES(?, ?, ?, ?, ?)",
                            request.getSource(), request.getExternalId(), request.getName(), licenseKey, request.getPlan());
        logger.info("Created {} with licenseKey {}", request, licenseKey);
        slackService.sendNotification(String.format("Handled `%s`", request), SlackService.Channel.BUSINESS_EVENTS);
        return licenseKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePlanForExternalId(@NonNull String externalId, @NonNull String newPlan) {
        CustomerData customerData = getCustomerDataByExternalId(externalId);

        if (newPlan.equals(customerData.getPricePlan().getName())) {
            logger.info("{} is already on plan '{}'", customerData, newPlan);
            return;
        }

        int count = jdbcTemplate.update("UPDATE customers SET plan = ? WHERE externalId = ?", newPlan, externalId);

        if (count == 0) {
            logger.warn("Failed to change plan for {} to '{}'", customerData, newPlan);
        } else {
            logger.info("Changed plan for {} to '{}'", customerData, newPlan);
            slackService.sendNotification(String.format("Changed plan for `%s` to '%s'", customerData, newPlan),
                                          SlackService.Channel.BUSINESS_EVENTS);

            count = jdbcTemplate.update("DELETE FROM price_plan_overrides WHERE customerId = ?", customerData.getCustomerId());
            if (count > 0) {
                PricePlanDefaults ppd = PricePlanDefaults.fromDatabaseName(newPlan);
                logger.warn("Removed price plan override, new effective price plan is {}", ppd);
                slackService.sendNotification("Removed price plan override, new effective price plan is `" + ppd + "`",
                                              SlackService.Channel.BUSINESS_EVENTS);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerByExternalId(String externalId) {
        CustomerData customerData = getCustomerDataByExternalId(externalId);

        long customerId = customerData.getCustomerId();

        deleteFromTable("invocations", customerId);
        deleteFromTable("methods", customerId);
        deleteFromTable("jvms", customerId);
        deleteFromTable("applications", customerId);
        deleteFromTable("users", customerId);
        deleteFromTable("agent_state", customerId);
        deleteFromTable("price_plan_overrides", customerId);
        deleteFromTable("customers", customerId);

        logger.info("Deleted customer {}", customerData);
        slackService.sendNotification("Deleted `" + customerData + "`", SlackService.Channel.BUSINESS_EVENTS);
    }

    private void deleteFromTable(final String table, long customerId) {
        String column = table.equals("customers") ? "id" : "customerId";
        int count = jdbcTemplate.update("DELETE FROM " + table + " WHERE " + column + " = ?", customerId);
        logger.debug("Deleted {} {}", count, table);
    }

    private CustomerData getCustomerData(String where_clause, java.io.Serializable identifier) {
        Map<String, Object> result = jdbcTemplate.queryForMap("SELECT " +
                                                                  " c.id, c.name, c.source, c.plan, c.collectionStartedAt, c" +
                                                                  ".trialPeriodEndsAt, " +
                                                                  " pp.createdBy, pp.note, pp.maxMethods, pp.maxNumberOfAgents, pp" +
                                                                  ".publishIntervalSeconds, pp.pollIntervalSeconds, " +
                                                                  " pp.retryIntervalSeconds " +
                                                                  "FROM customers c LEFT JOIN price_plan_overrides pp ON pp.customerId = " +
                                                                  "c.id " +
                                                                  "WHERE " + where_clause, identifier);

        String planName = (String) result.get("plan");
        Timestamp collectionStartedAt = (Timestamp) result.get("collectionStartedAt");
        Timestamp trialPeriodEndsAt = (Timestamp) result.get("trialPeriodEndsAt");
        PricePlanDefaults ppd = PricePlanDefaults.fromDatabaseName(planName);

        return CustomerData.builder()
                           .customerId((Long) result.get("id"))
                           .customerName((String) result.get("name"))
                           .source((String) result.get("source"))
                           .collectionStartedAt(collectionStartedAt != null ? Instant.ofEpochMilli(collectionStartedAt.getTime()) : null)
                           .trialPeriodEndsAt(trialPeriodEndsAt != null ? Instant.ofEpochMilli(trialPeriodEndsAt.getTime()) : null)
                           .pricePlan(
                               PricePlan.builder()
                                        .name(ppd.name())
                                        .overrideBy((String) result.get("createdBy"))
                                        .note((String) result.get("note"))
                                        .maxMethods(getOrDefault(result, "maxMethods", ppd.getMaxMethods()))
                                        .maxNumberOfAgents(getOrDefault(result, "maxNumberOfAgents", ppd.getMaxNumberOfAgents()))
                                        .pollIntervalSeconds(getOrDefault(result, "pollIntervalSeconds", ppd.getPollIntervalSeconds()))
                                        .publishIntervalSeconds(
                                            getOrDefault(result, "publishIntervalSeconds", ppd.getPublishIntervalSeconds()))
                                        .retryIntervalSeconds(getOrDefault(result, "retryIntervalSeconds", ppd.getRetryIntervalSeconds()))
                                        .maxCollectionPeriodDays(ppd.getMaxCollectionPeriodDays())
                                        .build())
                           .build();
    }

    private <T> T getOrDefault(Map<String, Object> result, String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) result.get(key);

        return value != null ? value : defaultValue;
    }

    private void doAssertNumberOfMethods(CustomerData customerData, long numberOfMethods) {
        logger.debug("Asserting {} methods for {}", numberOfMethods, customerData);

        PricePlan pp = customerData.getPricePlan();
        if (numberOfMethods > pp.getMaxMethods()) {
            throw new LicenseViolationException(
                String.format("Too many methods: %d. The plan '%s' has a limit of %d methods",
                              numberOfMethods, customerData.getPricePlan().getName(), pp.getMaxMethods()));
        }
    }

}
