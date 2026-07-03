package org.egov.infra.exception;

public class CustomUnknownHostException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CustomUnknownHostException(String message, Throwable cause) {
        super(message, cause);
    }
}
