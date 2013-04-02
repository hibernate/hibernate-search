package org.hibernate.search.test.service;

import java.util.Properties;

import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.impl.RAMDirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public class ProvidedServiceDirectoryProvider extends RAMDirectoryProvider {

	private ServiceManager serviceManager;
	private ProvidedService foo;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		super.initialize(
				directoryProviderName, properties, context
		);
		serviceManager = context.getServiceManager();
		foo = serviceManager.requestService( ProvidedServiceProvider.class, context );
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		if (foo == null) throw new RuntimeException( "service should be started" );
		if ( ! foo.isProvided() ) throw new RuntimeException( "provided service should be used" );
		super.start( indexManager );
	}

	@Override
	public void stop() {
		super.stop();
		serviceManager.releaseService( ProvidedServiceProvider.class );
	}
}
