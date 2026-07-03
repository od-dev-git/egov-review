package org.egov.infra.exception;

public class CustomResourceAccessException extends RuntimeException{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CustomResourceAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
