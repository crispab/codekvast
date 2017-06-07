/*
 * Copyright (c) 2015-2017 Hallin Information Technology AB
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
package io.codekvast.warehouse.customer;

import lombok.NonNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Collection;

/**
 * @author olle.hallin@crisp.se
 */
public interface CustomerService {

    /**
     * Translates a licenseKey to customer data.
     *
     * @param licenseKey The licenseKey to translate.
     * @return A CustomerData object.
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException iff the licenseKey was invalid.
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
     * Validates a received publication before accepting it.
     *
     * @param licenseKey      The license key to check
     * @param publicationSize The size of the publication.
     * @throws LicenseViolationException iff the license key is invalid
     */
    void assertPublicationSize(String licenseKey, int publicationSize) throws LicenseViolationException;

    /**
     * Checks that the database does not contain too many methods for a certain customer.
     *
     * @param customerId The customer ID
     */
    void assertDatabaseSize(long customerId) throws LicenseViolationException;

    /**
     * Query the database for all customers.
     *
     * @return A list of CustomerData objects.
     */
    Collection<CustomerData> getAllCustomers();
}
