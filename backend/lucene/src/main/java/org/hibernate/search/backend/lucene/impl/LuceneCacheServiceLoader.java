/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.util.Version;
import org.hibernate.search.backend.lucene.cache.spi.QueryCacheProvider;
import org.hibernate.search.backend.lucene.cache.spi.QueryCachingPolicyProvider;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.classpath.spi.AggregatedClassLoader;
import org.hibernate.search.engine.environment.classpath.spi.DefaultServiceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;

public class LuceneCacheServiceLoader {

	private static List<QueryCacheProvider> queryProviders;
	private static List<QueryCachingPolicyProvider> policyProviders;

	private LuceneCacheServiceLoader() {
	}

	private static synchronized Collection<QueryCacheProvider> getQueryProviders() {
		if ( queryProviders == null ) {
			queryProviders = new ArrayList<>();
			AggregatedClassLoader aggregatedClassLoader = AggregatedClassLoader.createDefault();
			ServiceResolver serviceResolver = DefaultServiceResolver.create( aggregatedClassLoader );
			Iterable<QueryCacheProvider> iterator = serviceResolver.loadJavaServices( QueryCacheProvider.class );
			for ( QueryCacheProvider provider : iterator ) {
				queryProviders.add( provider );
			}
		}
		return queryProviders;
	}

	private static synchronized Collection<QueryCachingPolicyProvider> getPolicyProviders() {
		if ( policyProviders == null ) {
			policyProviders = new ArrayList<>();
			AggregatedClassLoader aggregatedClassLoader = AggregatedClassLoader.createDefault();
			ServiceResolver serviceResolver = DefaultServiceResolver.create( aggregatedClassLoader );
			Iterable<QueryCachingPolicyProvider> iterator = serviceResolver.loadJavaServices( QueryCachingPolicyProvider.class );
			for ( QueryCachingPolicyProvider provider : iterator ) {
				policyProviders.add( provider );
			}
		}
		return policyProviders;
	}

	public static QueryCache findQueryCache(ConfigurationPropertySource propertySource, Version luceneVersion) {
		QueryCache cache = null;
		Collection<QueryCacheProvider> providers = getQueryProviders();
		for ( QueryCacheProvider provider : providers ) {
			cache = provider.getQueryCache( propertySource, luceneVersion );
			if ( cache != null ) {
				break;
			}
		}

		if ( cache == null ) {
			cache = IndexSearcher.getDefaultQueryCache();
		}

		return cache;
	}

	public static QueryCachingPolicy findQueryCachingPolicy(ConfigurationPropertySource propertySource, Version luceneVersion) {
		QueryCachingPolicy policy = null;
		Collection<QueryCachingPolicyProvider> providers = getPolicyProviders();
		for ( QueryCachingPolicyProvider provider : providers ) {
			policy = provider.getQueryCachingPolicy( propertySource, luceneVersion );
			if ( policy != null ) {
				break;
			}
		}

		if ( policy == null ) {
			policy = IndexSearcher.getDefaultQueryCachingPolicy();
		}

		return policy;
	}
}
