package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.spi.ServiceProvider;

/**
 * @author Emmanuel Bernard
 */
public class ProvidedServiceProvider implements ServiceProvider<ProvidedService> {
	private static volatile Boolean active = null;

	public void start(Properties properties) {
		throw new RuntimeException( "should not be started" );
	}

	public ProvidedService getService() {
		active = true;
		return new ProvidedService();
	}

	public void stop() {
		throw new RuntimeException( "should not be stopped" );
	}

	public static Boolean isActive() { return active; }
	public static void resetActive() { active = null; }
}
