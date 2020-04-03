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
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;

public class LuceneCacheServiceLoader {

	private LuceneCacheServiceLoader() {
	}

	private static synchronized Collection<QueryCacheProvider> getQueryProviders(BackendBuildContext context) {
		List<QueryCacheProvider> queryProviders = new ArrayList<>();
		ServiceResolver serviceResolver = context.getServiceResolver();
		Iterable<QueryCacheProvider> iterator = serviceResolver.loadJavaServices( QueryCacheProvider.class );
		for ( QueryCacheProvider provider : iterator ) {
			queryProviders.add( provider );
		}
		return queryProviders;
	}

	private static synchronized Collection<QueryCachingPolicyProvider> getPolicyProviders(BackendBuildContext context) {
		List<QueryCachingPolicyProvider> policyProviders = new ArrayList<>();
		ServiceResolver serviceResolver = context.getServiceResolver();
		Iterable<QueryCachingPolicyProvider> iterator = serviceResolver.loadJavaServices( QueryCachingPolicyProvider.class );
		for ( QueryCachingPolicyProvider provider : iterator ) {
			policyProviders.add( provider );
		}
		return policyProviders;
	}

	public static QueryCache findQueryCache(BackendBuildContext context, ConfigurationPropertySource propertySource, Version luceneVersion) {
		QueryCache cache = null;
		Collection<QueryCacheProvider> providers = getQueryProviders( context );
		for ( QueryCacheProvider provider : providers ) {
			cache = provider.getQueryCache( context, propertySource, luceneVersion );
			if ( cache != null ) {
				break;
			}
		}

		if ( cache == null ) {
			cache = IndexSearcher.getDefaultQueryCache();
		}

		return cache;
	}

	public static QueryCachingPolicy findQueryCachingPolicy(BackendBuildContext context, ConfigurationPropertySource propertySource, Version luceneVersion) {
		QueryCachingPolicy policy = null;
		Collection<QueryCachingPolicyProvider> providers = getPolicyProviders( context );
		for ( QueryCachingPolicyProvider provider : providers ) {
			policy = provider.getQueryCachingPolicy( context, propertySource, luceneVersion );
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
