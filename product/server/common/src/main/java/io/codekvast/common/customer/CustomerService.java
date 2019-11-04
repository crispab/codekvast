/*
 * Copyright (c) 2015-2019 Hallin Information Technology AB
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
package io.codekvast.common.customer;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.time.Instant;
import java.util.List;

/**
 * @author olle.hallin@crisp.se
 */
public interface CustomerService {

    interface Source {
        String HEROKU = "heroku";
    }

    /**
     * Translates a licenseKey to customer data.
     *
     * @param licenseKey The licenseKey to translate.
     * @return A CustomerData object.
     * @throws AuthenticationCredentialsNotFoundException iff the licenseKey was invalid.
     */
    CustomerData getCustomerDataByLicenseKey(@NonNull String licenseKey) throws AuthenticationCredentialsNotFoundException;

    /**
     * Translates an externalId to customer data.
     *
     * @param externalId The externalId to translate.
     * @return A CustomerData object.
     * @throws AuthenticationCredentialsNotFoundException iff the externalId was invalid.
     */
    CustomerData getCustomerDataByExternalId(@NonNull String externalId) throws AuthenticationCredentialsNotFoundException;

    /**
     * Translates a customerId to customer data.
     *
     * @param customerId The customer ID to translate.
     * @return A CustomerData object.
     * @throws AuthenticationCredentialsNotFoundException iff the customerId was invalid.
     */
    CustomerData getCustomerDataByCustomerId(long customerId) throws AuthenticationCredentialsNotFoundException;

    /**
     * Retrieve a list of customers which has a user with a certain email address
     *
     * @param email The user's email address.
     * @return A collection of CustomerData objects. Does never return null.
     */
    List<CustomerData> getCustomerDataByUserEmail(String email);

    /**
     * Validates a received publication before accepting it.
     *
     * @param customerData    The customer data.
     * @param publicationSize The size of the publication.
     * @throws LicenseViolationException iff the publication is larger than the customer's plan permits.
     */
    void assertPublicationSize(CustomerData customerData, int publicationSize) throws LicenseViolationException;

    /**
     * Checks that the database does not contain too many methods for a certain customer.
     *
     * @param customerId The customer ID
     * @throws LicenseViolationException iff the database contains more rows than the customer's plan permits.
     */
    void assertDatabaseSize(long customerId) throws LicenseViolationException;

    /**
     * Counts the number of methods this customer has.
     *
     * @param customerId The customer id
     * @return The number of methods that belong to this customer
     */
    int countMethods(long customerId);

    /**
     * Register that an agent has polled.
     *
     * It might result in the start of a trial period, if this is the first time and the customer has a price plan with a limitation on
     * trialPeriodDays.
     *
     * @param customerData The customer's data
     * @param polledAt     The instant the agent polled.
     * @return An updated customerData should there have been any changes. Does never return null.
     */
    CustomerData registerAgentPoll(CustomerData customerData, Instant polledAt);

    /**
     * Register that a user has logged in.
     *
     * @param request The login request
     */
    void registerLogin(LoginRequest request);

    /**
     * Adds a new customer
     *
     * @param request The add customer request data
     * @return A response containing the generated customerId and the unique license key
     */
    AddCustomerResponse addCustomer(AddCustomerRequest request);

    /**
     * Change plan for an existing customer
     *
     * @param externalId The external customer ID.
     * @param newPlan    The name of the new plan.
     */
    void changePlanForExternalId(String externalId, String newPlan);

    /**
     * Deletes a customer
     *
     * @param externalId The external id
     */
    void deleteCustomerByExternalId(String externalId);

    /**
     * Get a user's authorized roles.
     *
     * @param email The user's email address.
     * @return A list of role names. Does never return null.
     */
    List<String> getRoleNamesByUserEmail(String email);

    /**
     * @return A list of all CustomerData. Does never return null.
     */
    List<CustomerData> getCustomerData();

    /**
     * Update the app name and the contact email.
     *
     * @param appName      The new app name
     * @param contactEmail the new contact email
     * @param customerId   Identifies the customer
     */
    void updateAppDetails(String appName, String contactEmail, Long customerId);

    /**
     * Parameter object for {@link #addCustomer(AddCustomerRequest)}
     */
    @Value
    @Builder
    class AddCustomerRequest {
        @NonNull
        String source;

        @NonNull
        String externalId;

        @NonNull
        String name;

        @NonNull
        String plan;
    }

    @Value
    @Builder
    class AddCustomerResponse {
        @NonNull Long customerId;
        @NonNull String licenseKey;
    }

    /**
     * Parameter object for {@link #registerLogin(LoginRequest)}
     */
    @Value
    @Builder
    class LoginRequest {
        @NonNull
        Long customerId;

        @NonNull
        String email;

        @NonNull
        String source;
    }
}
