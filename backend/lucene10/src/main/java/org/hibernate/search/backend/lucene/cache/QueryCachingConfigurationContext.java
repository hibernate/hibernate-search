/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.cache;

import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.util.Version;

/**
 * A context allowing the configuration of query caching in a Lucene backend.
 */
public interface QueryCachingConfigurationContext {

	/**
	 * @return The Lucene version in use in the configured backend.
	 */
	Version luceneVersion();

	/**
	 * @param cache The {@link QueryCache} to use when searching.
	 */
	void queryCache(QueryCache cache);

	/**
	 * @param policy The {@link QueryCachingPolicy} to use when searching.
	 */
	void queryCachingPolicy(QueryCachingPolicy policy);

}
