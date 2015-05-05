/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.infinispan.spi;

import java.util.Properties;

/**
 * Configuration constants for the Infinispan integration
 *
 * @deprecated this implementation is now maintained by the Infinispan project: use {@link org.infinispan.hibernate.search.spi.InfinispanIntegration}
 * instead.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
@Deprecated
public class InfinispanIntegration {

	private InfinispanIntegration() {
		//not meant to be instantiated
	}

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_LOCKING_CACHENAME}.
	 */
	public static final String LOCKING_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.LOCKING_CACHENAME;

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_INDEXESDATA_CACHENAME}.
	 */
	public static final String DATA_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.DATA_CACHENAME;

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_INDEXESMETADATA_CACHENAME}.
	 */
	public static final String METADATA_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.METADATA_CACHENAME;

	/**
	 * Default name of the cache used to store Locking metadata
	 */
	public static final String DEFAULT_LOCKING_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.DEFAULT_LOCKING_CACHENAME;

	/**
	 * Default name of the cache used to store Index Data
	 */
	public static final String DEFAULT_INDEXESDATA_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.DEFAULT_INDEXESDATA_CACHENAME;

	/**
	 * Default name of the cache used to store Index MetaData
	 */
	public static final String DEFAULT_INDEXESMETADATA_CACHENAME = org.infinispan.hibernate.search.spi.InfinispanIntegration.DEFAULT_INDEXESMETADATA_CACHENAME;

	/**
	 * Configuration attribute to control if the writes to Index Metadata should be performed asynchronously.
	 * <p>
	 * Defaults to {@code false} if the backend is configured as synchronous and defaults to {@code true} if the backend
	 * is configured as asynchronous.
	 * <p>
	 * Setting this to {@code true} might improve performance but is highly experimental.
	 */
	public static final String WRITE_METADATA_ASYNC = org.infinispan.hibernate.search.spi.InfinispanIntegration.WRITE_METADATA_ASYNC;

	/**
	 * @param properties the Hibernate Search configuration
	 * @return the name of the Cache to be retrieved from the CacheManager to store Index Metadata
	 */
	public static String getMetadataCacheName(Properties properties) {
		return properties.getProperty( METADATA_CACHENAME, DEFAULT_INDEXESMETADATA_CACHENAME );
	}

	/**
	 * @param properties the Hibernate Search configuration
	 * @return the name of the Cache to be retrieved from the CacheManager to store Index Data
	 */
	public static String getDataCacheName(Properties properties) {
		return properties.getProperty( DATA_CACHENAME, DEFAULT_INDEXESDATA_CACHENAME );
	}

	/**
	 * @param properties the Hibernate Search configuration
	 * @return the name of the Cache to be retrieved from the CacheManager to store Locking metadata
	 */
	public static String getLockingCacheName(Properties properties) {
		return properties.getProperty( LOCKING_CACHENAME, DEFAULT_LOCKING_CACHENAME );
	}

}
