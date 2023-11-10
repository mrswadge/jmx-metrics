package com.sbs.jmx.server.exceptions;

public class CustomMBeanServerRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 5626902897008649200L;

	public CustomMBeanServerRuntimeException() {
	}

	public CustomMBeanServerRuntimeException( String message ) {
		super( message );
	}

	public CustomMBeanServerRuntimeException( Throwable cause ) {
		super( cause );
	}

	public CustomMBeanServerRuntimeException( String message, Throwable cause ) {
		super( message, cause );
	}

	public CustomMBeanServerRuntimeException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
		super( message, cause, enableSuppression, writableStackTrace );
	}

}
