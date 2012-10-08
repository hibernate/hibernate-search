package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.ServiceProvider;

/**
 * @author Emmanuel Bernard
 */
public class MyServiceProvider implements ServiceProvider<MyService> {

	private static volatile boolean active = false;
	private static volatile boolean simulateCircularDependency = false;
	private MyService foo;

	public void start(Properties properties, BuildContext context) {
		foo = new MyService();
		active = true;
		if ( simulateCircularDependency ) {
			context.getServiceManager().requestService( MyServiceProvider.class, context );
		}
	}

	public MyService getService() {
		return foo;
	}

	public void stop() {
		foo = null;
		active = false;
	}

	public static boolean isActive() { return active; }
	public static void resetActive() { active = false; }

	public static void setSimulateCircularDependency(boolean simulateCircularDependency) {
		MyServiceProvider.simulateCircularDependency = simulateCircularDependency;
	}

}
