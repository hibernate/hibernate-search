/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.infinispan;

import java.util.Properties;

/**
 * Configuration constants for the Infinispan integration
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class InfinispanIntegration {

	private InfinispanIntegration() {
		//not meant to be instantiated
	}

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_LOCKING_CACHENAME}.
	 */
	public static final String LOCKING_CACHENAME = "locking_cachename";

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_INDEXESDATA_CACHENAME}.
	 */
	public static final String DATA_CACHENAME = "data_cachename";

	/**
	 * Configuration attribute to override the value of {@link #DEFAULT_INDEXESMETADATA_CACHENAME}.
	 */
	public static final String METADATA_CACHENAME = "metadata_cachename";

	/**
	 * Default name of the cache used to store Locking metadata
	 */
	public static final String DEFAULT_LOCKING_CACHENAME = "LuceneIndexesLocking";

	/**
	 * Default name of the cache used to store Index Data
	 */
	public static final String DEFAULT_INDEXESDATA_CACHENAME = "LuceneIndexesData";

	/**
	 * Default name of the cache used to store Index MetaData
	 */
	public static final String DEFAULT_INDEXESMETADATA_CACHENAME = "LuceneIndexesMetadata";

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
