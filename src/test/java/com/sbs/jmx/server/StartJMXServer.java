package com.sbs.jmx.server;

import java.io.IOException;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class StartJMXServer {

	private static final Logger logger = LoggerFactory.getLogger( StartJMXServer.class );

	private CustomMBeanServer server;

	@BeforeSuite
	public void startJMXServer() {
		System.setProperty( "java.net.preferIPv4Stack", "true" );
		server = CustomMBeanServer.getInstance();
		server.setHost( null );
		server.setConnectorServerPort( 7115 );
		server.setLocateRegistryPort( 7114 );
		server.setContext( "github" );
		server.setDomain( "com.github" );
		server.start();
	}

	@Test
	public void queryMBeans() throws IOException {
		JMXConnector connector = JMXConnectorFactory.connect( server.getJMXServiceURL() );
		MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
		Set<ObjectInstance> beans = serverConnection.queryMBeans( null, null );
		beans.forEach( ( bean ) -> logger.info( "MBean: " + bean ) );
	}

	@Test
	public void queryNames() throws IOException {
		JMXConnector connector = JMXConnectorFactory.connect( server.getJMXServiceURL() );
		MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
		Set<ObjectName> names = serverConnection.queryNames( null, null );
		names.forEach( ( name ) -> logger.info( "ObjectName: " + name ) );
	}
}