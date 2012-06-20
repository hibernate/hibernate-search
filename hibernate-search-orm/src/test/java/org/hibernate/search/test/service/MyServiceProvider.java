package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;

/**
 * @author Emmanuel Bernard
 */
public class MyServiceProvider implements ServiceProvider<MyService> {
	private static volatile Boolean active = null;
	private static volatile boolean simulateCircularDependency = false;
	private MyService foo;

	public void start(Properties properties, BuildContext context) {
		foo = new MyService();
		active = Boolean.TRUE;
		if ( simulateCircularDependency ) {
			context.requestService( MyServiceProvider.class );
		}
	}

	public MyService getService() {
		return foo;
	}

	public void stop() {
		foo = null;
		active = Boolean.FALSE;
	}

	public static Boolean isActive() { return active; }
	public static void resetActive() { active = null; }

	public static void setSimulateCircularDependency(boolean simulateCircularDependency) {
		MyServiceProvider.simulateCircularDependency = simulateCircularDependency;
	}

}
