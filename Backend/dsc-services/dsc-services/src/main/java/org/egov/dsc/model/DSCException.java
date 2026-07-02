package org.egov.dsc.model;

@SuppressWarnings("serial")
public class DSCException  extends Exception{

	public DSCException() {
		super();
	}

	public DSCException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DSCException(String message, Throwable cause) {
		super(message, cause);
	}

	public DSCException(String message) {
		super(message);
	}

	public DSCException(Throwable cause) {
		super(cause);
	}

}
