// -------------------------------------------------------------------------------
//       Represents a duplicate active tax configuration error.
// -------------------------------------------------------------------------------
package com.frwss.system.tax.exception;

public class DuplicateTaxConfigurationException extends RuntimeException {

    /**
     * Creates an exception with a message describing the duplicate configuration.
     *
     * @param message description of the duplicate configuration error
     */
    public DuplicateTaxConfigurationException(String message) {
        super(message);
    }
}