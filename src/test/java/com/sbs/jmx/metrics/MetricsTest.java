package com.sbs.jmx.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jmx.ObjectNameFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sbs.jmx.server.CustomMBeanServer;

public class MetricsTest {

	private static final Logger logger = LoggerFactory.getLogger( MetricsTest.class );
	
	private CustomMBeanServer server;
	
	private MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate( MetricsTest.class.getName() );
	private JmxReporter reporter;

	class CustomObjectNameFactory implements ObjectNameFactory {
		private final ObjectMapper mapper = new ObjectMapper();
		
		private String escapeName( String s ) {
			return s == null ? null : s.replaceAll("[,=:\"*?!&<>\\\\\\n\\r ]", "_");
		}
		
		@Override
		public ObjectName createName(String type, String domain, String name) {
			String objectName = null;
			try { 
				@SuppressWarnings("unchecked")
				Map<String, String> attributes = mapper.readValue( name, Map.class );
				attributes.put( "name", escapeName( attributes.getOrDefault( "name", "ERROR_NAME_NOT_SET" ) ) );
		
				StringBuilder builder = new StringBuilder();
				
				for (Map.Entry<String, String> entry : attributes.entrySet()) {
					if (builder.length() > 0) {
						builder.append( "," );
					}
					String key = entry.getKey();
					String value = entry.getValue();
					if ( "name".equals( key ) ) {
						value = escapeName( value );
					}
					builder.append( key );
					builder.append( "=" );
					builder.append( value );
				}
				
				objectName = String.format( "%s:%s", domain, builder.toString() );
				return new ObjectName( objectName );
			} catch ( MalformedObjectNameException e ) {
				logger.error( String.format( "%s is not a valid ObjectName.", objectName ), e );
			} catch ( JsonMappingException e ) {
				logger.error( String.format( "Unable to map object name attributes.", objectName ), e );
			} catch ( JsonProcessingException e ) {
				logger.error( String.format( "Unable to process object name attributes.", objectName ), e );
			}
			return null;
		}
	}
	
	@BeforeClass
	public void init() {
		server = CustomMBeanServer.getInstance();
			
		reporter = JmxReporter
				.forRegistry( metricRegistry )
				.registerWith( server.getMBeanServer() )
				.inDomain( server.getDomain() )
				.createsObjectNamesWith( new CustomObjectNameFactory() )
				.build();
		
		reporter.start();
	}
	
	@Test
	public void recordMetrics() {
		Counter counter = metricRegistry.counter( createKey( "recordMetrics", "counter", this.getClass().getSimpleName(), "recordMetrics" ), () -> new Counter() );
		Histogram histogram = metricRegistry.histogram( createKey( "recordMetrics", "histogram", this.getClass().getSimpleName(), "recordMetrics" ), () -> new Histogram( new ExponentiallyDecayingReservoir() ) );
		
		counter.inc();
		counter.inc();
		counter.dec();
		counter.inc();
		counter.dec();
		counter.inc();
		counter.dec();
		counter.dec();

		histogram.update( (long) (Math.random() * 10000d) );
		
		metricRegistry.getMetrics().forEach( (key, metric) -> logger.info( "Key: " + key + ", Metric: " + metric ));
		
		server.listMBeans();
		server.listObjectNames();
	}

	private ObjectMapper mapper = new ObjectMapper();

	private String createKey( String name, String type, String classifier, String method ) {
		LinkedHashMap<String, String> key = Maps.newLinkedHashMap();
		key.put("name", name);
		key.put("type", type);
		key.put("class", classifier);
		key.put("method", method);
		try {
			return mapper.writeValueAsString( key );
		} catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}
	
}
