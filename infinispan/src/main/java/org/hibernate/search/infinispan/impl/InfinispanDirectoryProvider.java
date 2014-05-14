/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.store.Directory;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.CacheManagerService;
import org.hibernate.search.infinispan.InfinispanIntegration;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.infinispan.Cache;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A DirectoryProvider using Infinispan to store the Index. This depends on the
 * CacheManagerServiceProvider to get a reference to the Infinispan {@link EmbeddedCacheManager}.
 *
 * @author Sanne Grinovero
 */
public class InfinispanDirectoryProvider implements org.hibernate.search.store.DirectoryProvider<Directory> {

	private static final Log log = LoggerFactory.make( Log.class );

	private ServiceManager serviceManager;
	private String directoryProviderName;

	private String metadataCacheName;
	private String dataCacheName;
	private String lockingCacheName;
	private Integer chunkSize;

	private Directory directory;

	private EmbeddedCacheManager cacheManager;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.directoryProviderName = directoryProviderName;
		this.serviceManager = context.getServiceManager();
		this.cacheManager = serviceManager.requestService( CacheManagerService.class ).getEmbeddedCacheManager();
		metadataCacheName = InfinispanIntegration.getMetadataCacheName( properties );
		dataCacheName = InfinispanIntegration.getDataCacheName( properties );
		lockingCacheName = InfinispanIntegration.getLockingCacheName( properties );
		//Let it return null if it's not set, so that we can avoid applying any override.
		chunkSize = ConfigurationParseHelper.getIntValue( properties, "chunk_size" );
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		log.debug( "Starting InfinispanDirectory" );
		cacheManager.startCaches( metadataCacheName, dataCacheName, lockingCacheName );
		Cache<?,?> metadataCache = cacheManager.getCache( metadataCacheName );
		Cache<?,?> dataCache = cacheManager.getCache( dataCacheName );
		Cache<?,?> lockingCache = cacheManager.getCache( lockingCacheName );
		org.infinispan.lucene.directory.BuildContext directoryBuildContext = DirectoryBuilder
				.newDirectoryInstance( metadataCache, dataCache, lockingCache, directoryProviderName );
		if ( chunkSize != null ) {
			directoryBuildContext.chunkSize( chunkSize.intValue() );
		}
		directory = directoryBuildContext.create();
		DirectoryProviderHelper.initializeIndexIfNeeded( directory );
		log.debugf( "Initialized Infinispan index: '%s'", directoryProviderName );
	}

	@Override
	public void stop() {
		try {
			directory.close();
		}
		catch (IOException e) {
			log.unableToCloseLuceneDirectory( directory, e );
		}
		serviceManager.releaseService( CacheManagerService.class );
		log.debug( "Stopped InfinispanDirectory" );
	}

	@Override
	public Directory getDirectory() {
		return directory;
	}

	public EmbeddedCacheManager getCacheManager() {
		return cacheManager;
	}

}
