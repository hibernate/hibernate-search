/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cache.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.cache.QueryCachingConfigurationContext;

import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.util.Version;

public class LuceneQueryCachingContext implements QueryCachingConfigurationContext {

	private final Version luceneVersion;
	private QueryCache cache;
	private QueryCachingPolicy policy;

	public LuceneQueryCachingContext(Version luceneVersion) {
		this.luceneVersion = luceneVersion;
	}

	@Override
	public Version luceneVersion() {
		return this.luceneVersion;
	}

	@Override
	public void queryCache(QueryCache cache) {
		this.cache = cache;
	}

	public Optional<QueryCache> queryCache() {
		return Optional.ofNullable( cache );
	}

	@Override
	public void queryCachingPolicy(QueryCachingPolicy policy) {
		this.policy = policy;
	}

	public Optional<QueryCachingPolicy> queryCachingPolicy() {
		return Optional.ofNullable( policy );
	}

}
