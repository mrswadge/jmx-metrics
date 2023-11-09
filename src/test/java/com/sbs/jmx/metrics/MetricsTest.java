package com.sbs.jmx.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jmx.ObjectNameFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.sbs.jmx.server.CustomMBeanServer;
import com.sbs.jmx.server.exceptions.CustomMBeanServerRuntimeException;

public class MetricsTest {

	private static final Logger logger = LoggerFactory.getLogger( MetricsTest.class );
	
	private CustomMBeanServer server;
	
	private MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate( MetricsTest.class.getName() );
	private JmxReporter reporter;

	private static class CustomObjectNameFactory implements ObjectNameFactory {
		private final ObjectMapper mapper = new ObjectMapper();

		@Override
		public ObjectName createName( String type, String domain, String name ) {
			try {
				// Read the "name" variable as JSON. This is so we can set additional ObjectName attributes.
				Map<String, String> attributes = mapper.readValue( name, Map.class );
				
				StringBuilder sb = new StringBuilder();
				
				attributes.forEach( (k,v) -> {
					if ( sb.length() > 0 ) {
						sb.append(',');
					}
					sb.append(k);
					sb.append('=');
					sb.append(v);
				});
				sb.insert(0, ':');
				sb.insert(0, domain);

				return new ObjectName(sb.toString());
			} catch ( MalformedObjectNameException e ) {
				throw new CustomMBeanServerRuntimeException("Malformed Object Name", e);
			} catch ( JsonMappingException e ) {
				throw new CustomMBeanServerRuntimeException("Unable to map object name attributes.", e);
			} catch ( JsonProcessingException e ) {
				throw new CustomMBeanServerRuntimeException("Unable to process object name attributes.", e);
			}
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
	
	@Test( threadPoolSize = 50, invocationCount = 1000 )
	public void recordMetrics() throws InterruptedException {
		Counter counter = metricRegistry.counter( createKey( "recordMetrics", "counter", this.getClass().getSimpleName(), "recordMetrics" ), () -> new Counter() );
		Timer timer = metricRegistry.timer( createKey( "recordMetrics", "timer", this.getClass().getSimpleName(), "recordMetrics" ), () -> new Timer() );
		Context timeContext = timer.time();
		counter.inc();

		TimeUnit.MILLISECONDS.sleep( period(500) );
	
		counter.dec();
		timeContext.stop();
	}
	
	@AfterClass 
	public void report() {
		listMetrics();
		listMBeans();
	}
	
	public void listMetrics() {
		metricRegistry.getMetrics().forEach( (key, metric) -> logger.info( "Key: " + key + ", Metric: " + metric ));
	}

	public void listMBeans() {
		server.listMBeans();
		server.listObjectNames();
	}
	
	private long period(double max) {
		return Double.valueOf(Math.random() * max).longValue();
	}

	private ObjectMapper mapper = new ObjectMapper();

	private String createKey( String name, String type, String classifier, String method ) {
		LinkedHashMap<String, String> key = Maps.newLinkedHashMap();
		key.put("type", "metrics");
		key.put("integration", "external");
		key.put("name", name);
		key.put("metric", type);
		key.put("class", classifier);
		key.put("method", method);
		key.put("location", "webservice");
		
		try {
			return mapper.writeValueAsString( key );
		} catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}
	
}
