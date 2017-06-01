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
     * Validates a license key
     *
     * @param licenseKey The license key to check
     * @throws LicenseViolationException iff the license key is invalid
     */
    void checkLicenseKey(String licenseKey) throws LicenseViolationException;

    /**
     * Checks that the database does not contain too many methods for a certain customer.
     *
     * @param customerId The customer ID
     */
    void assertDatabaseSize(long customerId) throws LicenseViolationException;
}
