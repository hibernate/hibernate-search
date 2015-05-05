/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.cfg.spi.DirectoryProviderService;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.store.impl.FSMasterDirectoryProvider;
import org.hibernate.search.store.impl.FSSlaveDirectoryProvider;
import org.hibernate.search.store.impl.RAMDirectoryProvider;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Base class for custom {@link org.hibernate.search.cfg.spi.DirectoryProviderService} implementations
 *
 * @author gustavonalle
 */
public abstract class BaseDirectoryProviderService implements DirectoryProviderService {

	private static final Log LOG = LoggerFactory.make();
	private static final String CONFIG_KEY = "directory_provider";

	protected final Map<String, String> defaultProviderClasses = new HashMap<>( 5 );

	public BaseDirectoryProviderService() {
		defaultProviderClasses.put( "filesystem", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-master", FSMasterDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-slave", FSSlaveDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "ram", RAMDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "infinispan", "org.infinispan.hibernate.search.spi.InfinispanDirectoryProvider" );
	}

	@Override
	public DirectoryProvider create(Properties indexProps, String indexName, BuildContext context) {
		String className = indexProps.getProperty( CONFIG_KEY, "" ).trim();
		if ( className.isEmpty() ) {
			return initialize( getDefault().getName(), indexName, indexProps, context );
		}
		String fullClassName = toFullyQualifiedClassName( className );
		return initialize( fullClassName, indexName, indexProps, context );
	}

	protected DirectoryProvider<?> initialize(String fullClassName, String indexName, Properties indexProps, BuildContext context) {
		ServiceManager serviceManager = context.getServiceManager();
		DirectoryProvider provider = ClassLoaderHelper.instanceFromName(
				DirectoryProvider.class,
				fullClassName,
				"directory provider",
				serviceManager
		);
		try {
			provider.initialize( indexName, indexProps, context );
		}
		catch (Exception e) {
			throw LOG.cannotInitializeDirectoryProvider( provider.getClass(), indexName, e );
		}
		return provider;
	}

}
