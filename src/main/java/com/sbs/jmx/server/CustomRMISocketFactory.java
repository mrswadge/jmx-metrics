package com.sbs.jmx.server;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRMISocketFactory extends RMISocketFactory implements Serializable {

	private static final long serialVersionUID = 603207422822920178L;

	private static final Logger logger = LoggerFactory.getLogger( CustomRMISocketFactory.class );

	private String host;
	private int port;
	private int backlog = 50;

	/**
	 * No override, just use a wildcard.
	 */
	public CustomRMISocketFactory() {
		// NOP
	}

	/**
	 * Bind server socket to specific address and port.
	 */
	public CustomRMISocketFactory( String host, int port ) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Bind server socket to specific address and port with backlog.
	 */
	public CustomRMISocketFactory( String host, int port, int backlog ) {
		this.host = host;
		this.port = port;
		this.backlog = backlog;
	}

	public Socket createSocket( String host, int port ) throws IOException {
		InetAddress address = InetAddress.getByName( host );
		logger.debug( String.format( "Creating socket to host %s (%s:%d)", host, address, port ) );
		return new Socket( address, port );
	}

	public ServerSocket createServerSocket( int port ) throws IOException {
		InetAddress bindAddress;
		int bindPort;

		// If the host and port are overridden, then map the bind address.
		if ( StringUtils.isNotBlank( this.host ) && this.port != 0 ) {
			bindAddress = InetAddress.getByName( this.host );
			bindPort = this.port;
			logger.info( String.format( "Will bind to overridden address %s:%d (via %s)", bindAddress, bindPort, this.host ) );
		} else {
			bindAddress = InetAddress.getByName( "0.0.0.0" );
			bindPort = port;
			logger.info( String.format( "Will bind to wildcard address %s:%d", bindAddress, bindPort ) );
		}

		logger.info( String.format( "Creating server socket %s:%d with backlog %d", bindAddress, bindPort, backlog ) );
		return new ServerSocket( bindPort, backlog, bindAddress );
	}
}
