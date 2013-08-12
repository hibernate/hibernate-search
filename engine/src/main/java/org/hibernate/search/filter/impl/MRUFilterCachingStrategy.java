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
package org.hibernate.search.filter.impl;

import java.util.Properties;

import org.apache.lucene.search.Filter;
import org.hibernate.search.Environment;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.SoftLimitMRUCache;

/**
 * Keep the most recently used Filters in the cache
 * The cache is at least as big as <code>hibernate.search.filter.cache_strategy.size</code>
 * Above this limit, Filters are kept as soft references
 *
 * @author Emmanuel Bernard
 */
public class MRUFilterCachingStrategy implements FilterCachingStrategy {
	private static final int DEFAULT_SIZE = 128;
	private SoftLimitMRUCache cache;
	private static final String SIZE = Environment.FILTER_CACHING_STRATEGY + ".size";

	/**
	 * Under memory pressure the JVM will release all Soft references,
	 * so pushing it too high will invalidate all eventually useful other caches.
	 */
	private static final int HARD_TO_SOFT_RATIO = 15;

	@Override
	public void initialize(Properties properties) {
		int size = ConfigurationParseHelper.getIntValue( properties, SIZE, DEFAULT_SIZE );
		cache = new SoftLimitMRUCache( size, size * HARD_TO_SOFT_RATIO );
	}

	@Override
	public Filter getCachedFilter(FilterKey key) {
		return (Filter) cache.get( key );
	}

	@Override
	public void addCachedFilter(FilterKey key, Filter filter) {
		cache.put( key, filter );
	}
}
