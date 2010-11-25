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
package org.hibernate.search.infinispan;

import java.io.IOException;
import java.util.Properties;

import org.infinispan.Cache;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.DirectoryProviderHelper;
import org.hibernate.search.util.LoggerFactory;

/**
 * A DirectoryProvider using Infinispan to store the Index. This depends on the
 * CacheManagerServiceProvider to get a reference to the Infinispan {@link EmbeddedCacheManager}.
 *
 * @author Sanne Grinovero
 */
public class InfinispanDirectoryProvider implements org.hibernate.search.store.DirectoryProvider<InfinispanDirectory> {

	private static final Logger log = LoggerFactory.make();

	public static final String DEFAULT_LOCKING_CACHENAME = "LuceneIndexesLocking";

	public static final String DEFAULT_INDEXESDATA_CACHENAME = "LuceneIndexesData";

	public static final String DEFAULT_INDEXESMETADATA_CACHENAME = "LuceneIndexesMetadata";

	private BuildContext context;
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
		this.context = context;
		metadataCacheName = properties.getProperty( "metadata_cachename", DEFAULT_INDEXESMETADATA_CACHENAME );
		dataCacheName = properties.getProperty( "data_cachename", DEFAULT_INDEXESDATA_CACHENAME );
		lockingCacheName = properties.getProperty( "locking_cachename", DEFAULT_LOCKING_CACHENAME );
		chunkSize = ConfigurationParseHelper.getIntValue(
				properties, "chunk_size", InfinispanDirectory.DEFAULT_BUFFER_SIZE
		);
	}

	@Override
	public void start() {
		log.debug( "Starting InfinispanDirectory" );
		cacheManager = context.requestService( CacheManagerServiceProvider.class );
		Cache metadataCache = cacheManager.getCache( metadataCacheName );
		Cache dataCache = cacheManager.getCache( dataCacheName );
		Cache lockingCache = cacheManager.getCache( lockingCacheName );
		directory = new InfinispanDirectory( metadataCache, dataCache, lockingCache, directoryProviderName, chunkSize );
		DirectoryProviderHelper.initializeIndexIfNeeded( directory );
		log.debug( "Initialized Infinispan index: '{}'", directoryProviderName );
	}

	@Override
	public void stop() {
		try {
			directory.close();
		}
		catch ( IOException e ) {
			// should never happen, #close() had a wrong signature
			throw new SearchException( e );
		}
		finally {
			context.releaseService( CacheManagerServiceProvider.class );
		}
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
