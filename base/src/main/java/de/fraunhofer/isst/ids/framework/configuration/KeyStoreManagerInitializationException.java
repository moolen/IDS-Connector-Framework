package de.fraunhofer.isst.ids.framework.configuration;

/**
 * Exception which is thrown, when the {@link KeyStoreManager} cannot be initialized
 */
public class KeyStoreManagerInitializationException extends Exception{

    /**
     * Create a KeyStoreManagerInitializationException with a given Message and Cause
     *
     * @param message error message of the exception
     * @param cause cause for the exception
     */
    public KeyStoreManagerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
