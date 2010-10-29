package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.RAMDirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public class ServiceDirectoryProvider extends RAMDirectoryProvider {
	private BuildContext context;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		super.initialize(
				directoryProviderName, properties, context
		);
		this.context = context;
	}

	@Override
	public void start() {
		final MyService foo = context.requestService( MyServiceProvider.class );
		if (foo == null) throw new RuntimeException( "service should be started" );
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		context.releaseService( MyServiceProvider.class );
	}
}
