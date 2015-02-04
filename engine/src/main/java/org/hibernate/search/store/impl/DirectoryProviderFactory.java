/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Create a Lucene directory provider which can be configured
 * through the following properties:
 * <ul>
 * <li><i>hibernate.search.default.*</i></li>
 * <li><i>hibernate.search.&lt;indexname&gt;.*</i>,</li>
 * </ul>where <i>&lt;indexname&gt;</i> properties have precedence over default ones.
 * <p/>
 * The implementation is described by
 * <i>hibernate.search.[default|indexname].directory_provider</i>.
 * If none is defined the default value is FSDirectory.
 *
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public final class DirectoryProviderFactory {

	private static final Log LOG = LoggerFactory.make();

	private static final Map<String, String> defaultProviderClasses;

	private DirectoryProviderFactory() {
		//not allowed
	}

	static {
		defaultProviderClasses = new HashMap<String, String>( 6 );
		defaultProviderClasses.put( "", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem", FSDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-master", FSMasterDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "filesystem-slave", FSSlaveDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "ram", RAMDirectoryProvider.class.getName() );
		defaultProviderClasses.put( "infinispan", "org.hibernate.search.infinispan.spi.InfinispanDirectoryProvider" );
	}

	public static DirectoryProvider<?> createDirectoryProvider(String indexName, Properties indexProps, WorkerBuildContext context) {
		String className = indexProps.getProperty( "directory_provider", "" );
		String maybeShortCut = className.toLowerCase();

		DirectoryProvider<?> provider;
		ServiceManager serviceManager = context.getServiceManager();
		//try and use the built-in shortcuts before loading the provider as a fully qualified class name
		if ( defaultProviderClasses.containsKey( maybeShortCut ) ) {
			String fullClassName = defaultProviderClasses.get( maybeShortCut );
			provider = ClassLoaderHelper.instanceFromName(
					DirectoryProvider.class,
					fullClassName,
					"directory provider",
					serviceManager
			);
		}
		else {
			provider = ClassLoaderHelper.instanceFromName(
					DirectoryProvider.class,
					className,
					"directory provider",
					serviceManager
			);
		}
		try {
			provider.initialize( indexName, indexProps, context );
		}
		catch (Exception e) {
			throw LOG.cannotInitializeDirectoryProvider( provider.getClass(), indexName, e );
		}
		return provider;
	}

}
