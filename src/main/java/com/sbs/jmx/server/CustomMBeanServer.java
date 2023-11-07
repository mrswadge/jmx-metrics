package com.sbs.jmx.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIConnectorServer;

import com.sbs.jmx.server.exceptions.CustomMBeanServerRuntimeException;
import com.sbs.jmx.server.exceptions.InvalidCustomMBeanServerConfigurationException;

public class CustomMBeanServer {
	private InetAddress host;
	private int locateRegistryPort;
	private int jmxConnectorServerPort;
	private String context = "custom";
	
	private MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer("custom");
	private Registry registry;
	private JMXConnectorServer connectorServer;
	
	public CustomMBeanServer( InetAddress host, int locateRegistryPort, int jmxConnectorServerPort ) throws InvalidCustomMBeanServerConfigurationException {
		if ( locateRegistryPort == jmxConnectorServerPort || locateRegistryPort < 0 || jmxConnectorServerPort < 0 ) {
			throw new InvalidCustomMBeanServerConfigurationException( "Provided ports collide." );
		}

		this.host = host;
		this.locateRegistryPort = locateRegistryPort;
		this.jmxConnectorServerPort = jmxConnectorServerPort;
	}
	
	public CustomMBeanServer( InetAddress host, int locateRegistryPort, int jmxConnectorServerPort, String context ) throws InvalidCustomMBeanServerConfigurationException {
		this( host, locateRegistryPort, jmxConnectorServerPort );
		if ( context == null || context.length() == 0 ) {
			throw new InvalidCustomMBeanServerConfigurationException( "The provided context was null or empty." );
		} 
		this.context = context;
	}
	
	private String normalizeAddress( InetAddress host, int port ) {
		StringBuilder sb = new StringBuilder();
		sb.append( host.getHostName() );
		if ( port > 0 ) {
			sb.append( ':' );
			sb.append( port );
		}
		return sb.toString();
	}
	
	public String getServerUrl() {
		String locate = normalizeAddress( host, locateRegistryPort );
		String connector = normalizeAddress( host, jmxConnectorServerPort );
		return String.format( "service:jmx:rmi://%s/jndi/rmi://%s/%s", locate, connector, context );
	}
	
	public JMXServiceURL getJMXServiceURL() {
		String url = getServerUrl();
		try {
			return new JMXServiceURL( url );
		} catch (MalformedURLException e) {
			throw new CustomMBeanServerRuntimeException( String.format( "Custom MBean Server URL '%s' is invalid.", url ), e );
		}
	}
	
	public void start() {
		try {
			startLocateRegistry();
			startJMXConnectorServer();
		} catch (IOException e) {
			throw new CustomMBeanServerRuntimeException( "Could not start JMX Connector Server.", e );
		}
	}
	
	private void startLocateRegistry() throws RemoteException {
		RMIServerSocketFactory locateRegistrySocketFactory = port -> new ServerSocket( CustomMBeanServer.this.locateRegistryPort, 50, CustomMBeanServer.this.host );
		this.registry = LocateRegistry.createRegistry( locateRegistryPort, null, locateRegistrySocketFactory );
	}

	private void startJMXConnectorServer() throws IOException {
		RMIServerSocketFactory jmxConnectorSocketFactory = port -> new ServerSocket( CustomMBeanServer.this.jmxConnectorServerPort, 50, CustomMBeanServer.this.host );

		JMXServiceURL url = getJMXServiceURL();

		Map<String, Object> environment = new HashMap<>();
		environment.put( RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, jmxConnectorSocketFactory );
		
		this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer( url, environment, mbeanServer );
		this.connectorServer.start();
	}
	
	public Registry getRegistry() {
		if ( registry == null ) {
			throw new CustomMBeanServerRuntimeException( "Locate Registry not started." );
		}
		return registry;
	}
	
	public JMXConnectorServer getConnectorServer() {
		if ( connectorServer == null ) {
			throw new CustomMBeanServerRuntimeException( "JMX Connector Server not started." );
		}
		return connectorServer;
	}
	
	public static void main(String[] args) throws Exception {
		 // Set the system property to prefer IPv4 over IPv6
	    System.setProperty( "java.net.preferIPv4Stack", "true" );
	    
		CustomMBeanServer server = new CustomMBeanServer( InetAddress.getLoopbackAddress(), 7771, 7772 );
		server.start();
		
		JMXConnector connector = JMXConnectorFactory.connect( server.getJMXServiceURL() );
		MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
		Set<ObjectInstance> beans = serverConnection.queryMBeans(null, null);
		beans.forEach( (bean) -> System.out.println( bean ) );
	}
}
