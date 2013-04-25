/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.infinispan.impl;

import java.util.Properties;

import org.infinispan.Cache;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.EmbeddedCacheManager;

import org.hibernate.search.engine.ServiceManager;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.infinispan.CacheManagerServiceProvider;
import org.hibernate.search.infinispan.InfinispanIntegration;
import org.hibernate.search.store.impl.DirectoryProviderHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.infinispan.logging.impl.Log;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A DirectoryProvider using Infinispan to store the Index. This depends on the
 * CacheManagerServiceProvider to get a reference to the Infinispan {@link EmbeddedCacheManager}.
 *
 * @author Sanne Grinovero
 */
public class InfinispanDirectoryProvider implements org.hibernate.search.store.DirectoryProvider<InfinispanDirectory> {

	private static final Log log = LoggerFactory.make( Log.class );

	/**
	 * Use {@link InfinispanIntegration.DEFAULT_LOCKING_CACHENAME} instead.
	 */
	@Deprecated
	public static final String DEFAULT_LOCKING_CACHENAME = InfinispanIntegration.DEFAULT_LOCKING_CACHENAME;

	/**
	 * Use {@link InfinispanIntegration.DEFAULT_INDEXESDATA_CACHENAME} instead.
	 */
	@Deprecated
	public static final String DEFAULT_INDEXESDATA_CACHENAME = InfinispanIntegration.DEFAULT_INDEXESDATA_CACHENAME;

	/**
	 * Use {@link InfinispanIntegration.DEFAULT_LOCKING_CACHENAME} instead.
	 */
	@Deprecated
	public static final String DEFAULT_INDEXESMETADATA_CACHENAME = InfinispanIntegration.DEFAULT_LOCKING_CACHENAME;

	private ServiceManager serviceManager;
	private String directoryProviderName;

	private String metadataCacheName;
	private String dataCacheName;
	private String lockingCacheName;
	private int chunkSize;

	private InfinispanDirectory directory;

	private EmbeddedCacheManager cacheManager;

	@Override
	public void initialize(String directoryProviderName, Properties properties, BuildContext context) {
		this.directoryProviderName = directoryProviderName;
		this.serviceManager = context.getServiceManager();
		this.cacheManager = serviceManager.requestService( CacheManagerServiceProvider.class, context );
		metadataCacheName = InfinispanIntegration.getMetadataCacheName( properties );
		dataCacheName = InfinispanIntegration.getDataCacheName( properties );
		lockingCacheName = InfinispanIntegration.getLockingCacheName( properties );
		chunkSize = ConfigurationParseHelper.getIntValue(
				properties, "chunk_size", InfinispanDirectory.DEFAULT_BUFFER_SIZE
		);
	}

	@Override
	public void start(DirectoryBasedIndexManager indexManager) {
		log.debug( "Starting InfinispanDirectory" );
		cacheManager.startCaches( metadataCacheName, dataCacheName, lockingCacheName );
		Cache<?,?> metadataCache = cacheManager.getCache( metadataCacheName );
		Cache<?,?> dataCache = cacheManager.getCache( dataCacheName );
		Cache<?,?> lockingCache = cacheManager.getCache( lockingCacheName );
		directory = new InfinispanDirectory( metadataCache, dataCache, lockingCache, directoryProviderName, chunkSize );
		DirectoryProviderHelper.initializeIndexIfNeeded( directory );
		log.debugf( "Initialized Infinispan index: '%s'", directoryProviderName );
	}

	@Override
	public void stop() {
		directory.close();
		serviceManager.releaseService( CacheManagerServiceProvider.class );
		log.debug( "Stopped InfinispanDirectory" );
	}

	@Override
	public InfinispanDirectory getDirectory() {
		return directory;
	}

	public EmbeddedCacheManager getCacheManager() {
		return cacheManager;
	}

}
