package com.sbs.jmx.server.exceptions;

public class InvalidCustomMBeanServerConfigurationException extends Exception {

	private static final long serialVersionUID = 2890525637561714051L;

	public InvalidCustomMBeanServerConfigurationException() {
	}

	public InvalidCustomMBeanServerConfigurationException( String message ) {
		super( message );
	}

	public InvalidCustomMBeanServerConfigurationException( Throwable cause ) {
		super( cause );
	}

	public InvalidCustomMBeanServerConfigurationException( String message, Throwable cause ) {
		super( message, cause );
	}

	public InvalidCustomMBeanServerConfigurationException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
		super( message, cause, enableSuppression, writableStackTrace );
	}

}
