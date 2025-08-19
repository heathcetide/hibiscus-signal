package com.hibiscus.signal.exceptions;

/**
 * Exception thrown when a signal processing error occurs.
 * <p>
 * This class includes an error code to distinguish different error types,
 * making it suitable for structured exception handling in business logic.
 */
public class SignalProcessingException extends Exception {

    /** Error code used to identify the specific type of signal failure. */
    private final int errorCode;

    /**
     * Constructs a new {@code SignalProcessingException} with a specified message and error code.
     *
     * @param message   a human-readable description of the error
     * @param errorCode the custom application-specific error code
     */
    public SignalProcessingException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new {@code SignalProcessingException} with a message, error code, and root cause.
     *
     * @param message   a human-readable description of the error
     * @param errorCode the custom application-specific error code
     * @param cause     the underlying cause of the exception
     */
    public SignalProcessingException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the error code as an integer
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a string representation of the exception, including the error code,
     * message, and cause if available.
     *
     * @return a detailed string describing the exception
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SignalProcessingException [")
                .append("errorCode=").append(errorCode)
                .append(", message=").append(getMessage());

        if (getCause() != null) {
            sb.append(", cause=").append(getCause());
        }

        sb.append("]");
        return sb.toString();
    }
}
