package org.egov.dsc.model;

@SuppressWarnings("serial")
public class DSCException  extends Exception{
	
	private boolean dsc;

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
	
    public DSCException(String message, boolean dsc) {
        super(message);
        this.dsc = dsc;
    }

    public boolean isDsc() {
        return dsc;
    }

}
