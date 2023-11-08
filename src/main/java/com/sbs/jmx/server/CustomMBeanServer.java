package com.sbs.jmx.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Hashtable;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sbs.jmx.server.exceptions.CustomMBeanServerRuntimeException;
import com.sbs.jmx.server.exceptions.InvalidCustomMBeanServerConfigurationException;

public class CustomMBeanServer {
	
	private static final Logger logger = LoggerFactory.getLogger( CustomMBeanServer.class );
	
	private MBeanServer mbeanServer;
	private Registry registry;
	private JMXConnectorServer server;

	private String host;
	private int connectorServerPort = 7114;
	private int locateRegistryPort = 7115;
	private String context = "sbs";
	private String domain = CustomMBeanServer.class.getName();
	
	private boolean started = false;
	
	private static final CustomMBeanServer instance = new CustomMBeanServer();
	
	private CustomMBeanServer() {
	}
	
	public static CustomMBeanServer getInstance() {
		return instance;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void setContext(String context) {
		if ( isStarted() ) {
			throw new CustomMBeanServerRuntimeException( "Context cannot be changed as the server has already started." );
		}
		this.context = context;
	}
	
	public String getContext() {
		return context;
	}
	
	public void setDomain(String domain) {
		if ( isStarted() ) {
			throw new CustomMBeanServerRuntimeException( "Domain cannot be changed as the server has already started." );
		}
		this.domain = domain;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setHost(String host) {
		if ( isStarted() ) {
			throw new CustomMBeanServerRuntimeException( "Host cannot be changed as the server has already started." );
		}
		this.host = host;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setConnectorServerPort(int connectorServerPort) {
		if ( isStarted() ) {
			throw new CustomMBeanServerRuntimeException( "Connector server port cannot be changed as the server has already started." );
		}
		this.connectorServerPort = connectorServerPort;
	}
	
	public int getConnectorServerPort() {
		return connectorServerPort;
	}
	
	public void setLocateRegistryPort(int locateRegistryPort) {
		if ( isStarted() ) {
			throw new CustomMBeanServerRuntimeException( "Locate registry port cannot be changed as the server has already started." );
		}
		this.locateRegistryPort = locateRegistryPort;
	}
	
	public int getLocateRegistryPort() {
		return locateRegistryPort;
	}

	private String normalizeAddress( String host, int port ) {
		StringBuilder sb = new StringBuilder();
		if ( StringUtils.isBlank( host ) ) {
			host = "localhost";
		}
		sb.append( host );
		if ( port > 0 ) {
			sb.append( ':' );
			sb.append( port );
		}
		return sb.toString();
	}
	
	public JMXServiceURL getJMXServiceURL() {
		String locateAddr = normalizeAddress( host, locateRegistryPort );
		String connectorAddr = normalizeAddress( host, connectorServerPort );
		String url = String.format( "service:jmx:rmi://%s/jndi/rmi://%s/%s", connectorAddr, locateAddr, context );
		try {
			return new JMXServiceURL( url );
		} catch (MalformedURLException e) {
			throw new CustomMBeanServerRuntimeException( String.format( "Custom MBean Server URL '%s' is invalid.", url ), e );
		}
	}
	
	public void start() {
		try {
			validate();
			this.mbeanServer = createMBeanServer();
			this.registry = createLocateRegistry();
			this.server = createJMXConnectorServer();
			this.started = true;
		} catch (IOException | InvalidCustomMBeanServerConfigurationException e) {
			throw new CustomMBeanServerRuntimeException( "Could not start JMX Connector Server.", e );
		}
	}
	
	private void validate() throws InvalidCustomMBeanServerConfigurationException {
		if ( StringUtils.isBlank( domain ) ) {
			throw new InvalidCustomMBeanServerConfigurationException( "MBeanServer domain cannot be blank." );
		}
		if ( locateRegistryPort == connectorServerPort || locateRegistryPort < 0 || connectorServerPort < 0 ) {
			throw new InvalidCustomMBeanServerConfigurationException( "Provided ports are invalid." );
		}
		if ( StringUtils.isBlank( context ) ) {
			throw new InvalidCustomMBeanServerConfigurationException( "The provided context was null or empty." );
		} 
	}

	private MBeanServer createMBeanServer() {
		return MBeanServerFactory.createMBeanServer( domain );
	}
	
	private Registry createLocateRegistry() throws RemoteException {
		RMISocketFactory csf = null;
		RMISocketFactory ssf = new CustomRMISocketFactory( host, locateRegistryPort, 50 );

		return LocateRegistry.createRegistry( locateRegistryPort, csf, ssf );
	}

	private JMXConnectorServer createJMXConnectorServer() throws IOException {
		JMXServiceURL url = getJMXServiceURL();
		logger.info( String.format( "JMX: Server initialising: %s", String.valueOf( url ) ) );
		
		RMISocketFactory ssf = new CustomRMISocketFactory( host, connectorServerPort, 50 );

		Hashtable<String, Object> env = new Hashtable<>();
		env.put( RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf );
		
		logger.info( String.format( "MBeanServer domain: %s", mbeanServer.getDefaultDomain() ) );
		JMXConnectorServer connectorServer = JMXConnectorServerFactory.newJMXConnectorServer( url, env, mbeanServer );
		connectorServer.start();
		
		logger.info( String.format( "JMX: Server started: %s", String.valueOf( connectorServer.getAddress() ) ) );
		
		return connectorServer;
	}

	public MBeanServer getMBeanServer() {
		if ( mbeanServer == null ) {
			throw new CustomMBeanServerRuntimeException( "MBeanServer is not started." );
		}
		return mbeanServer;
	}
	
	public Registry getRegistry() {
		if ( registry == null ) {
			throw new CustomMBeanServerRuntimeException( "Locate Registry is not started." );
		}
		return registry;
	}

	public JMXConnectorServer getServer() {
		if ( server == null ) {
			throw new CustomMBeanServerRuntimeException( "JMX Connector Server is not started." );
		}
		return server;
	}
	
	public void listMBeans() {
		if ( mbeanServer == null ) {
			throw new CustomMBeanServerRuntimeException( "MBeanServer is not started." );
		}
		Set<ObjectInstance> beans = mbeanServer.queryMBeans( null, null );
		beans.forEach( (bean) -> logger.info( "MBean: " + bean ) );
	}
	
	public void listObjectNames() {
		if ( mbeanServer == null ) {
			throw new CustomMBeanServerRuntimeException( "MBeanServer is not started." );
		}
		Set<ObjectName> names = mbeanServer.queryNames( null, null );
		names.forEach( (name) -> logger.info( "ObjectName: " + name ) );	
	}
	
	public static void main(String[] args) throws Exception {
		 // Set the system property to prefer IPv4 over IPv6
	    System.setProperty( "java.net.preferIPv4Stack", "true" );
		CustomMBeanServer server = CustomMBeanServer.getInstance();
		server.setHost( null );
		server.setConnectorServerPort( 7771 );
		server.setLocateRegistryPort( 7772 );
		server.setContext( "github" );
		server.setDomain( "com.github" );
		server.start();
		
		JMXConnector connector = JMXConnectorFactory.connect( server.getJMXServiceURL() );
		MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
		Set<ObjectInstance> beans = serverConnection.queryMBeans(null, null);
		beans.forEach( (bean) -> System.out.println( bean ) );
	}
}
