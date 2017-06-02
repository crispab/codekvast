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

/**
 * @author olle.hallin@crisp.se
 */
public interface CustomerService {

    /**
     * Translates a licenseKey to customer data, which governs what the customer is licensed to do.
     *
     * @param licenseKey The licenseKey to check.
     * @return A CustomerData object.
     * @throws LicenseViolationException iff the licenseKey was invalid.
     */
    CustomerData getCustomerDataByLicenseKey(@NonNull String licenseKey) throws LicenseViolationException;

    /**
     * Translates a customerId to customer data, which governs what the customer is licensed to do.
     *
     * @param customerId The customer ID
     * @return A CustomerData object.
     * @throws LicenseViolationException iff the licenseKey was invalid.
     */
    CustomerData getCustomerDataByCustomerId(long customerId) throws LicenseViolationException;

    /**
     * Validates a received publication before accepting it.
     *
     * @param licenseKey The license key to check
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
}
