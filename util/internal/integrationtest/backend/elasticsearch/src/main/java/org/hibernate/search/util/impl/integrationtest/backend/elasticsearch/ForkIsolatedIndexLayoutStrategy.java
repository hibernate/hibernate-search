/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.SimpleIndexLayoutStrategy;
import org.hibernate.search.util.impl.integrationtest.common.TestForkPrefix;

public final class ForkIsolatedIndexLayoutStrategy implements IndexLayoutStrategy {

	public static final IndexLayoutStrategy INSTANCE = new ForkIsolatedIndexLayoutStrategy();

	public static final String NAME = "fork-isolated";

	private static final String PREFIX = TestForkPrefix.PREFIX;

	private final SimpleIndexLayoutStrategy delegate = new SimpleIndexLayoutStrategy();

	@Override
	public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
		return delegate.createInitialElasticsearchIndexName( PREFIX + hibernateSearchIndexName );
	}

	@Override
	public String createWriteAlias(String hibernateSearchIndexName) {
		return delegate.createWriteAlias( PREFIX + hibernateSearchIndexName );
	}

	@Override
	public String createReadAlias(String hibernateSearchIndexName) {
		return delegate.createReadAlias( PREFIX + hibernateSearchIndexName );
	}

	@Override
	public String extractUniqueKeyFromHibernateSearchIndexName(String hibernateSearchIndexName) {
		return PREFIX + delegate.extractUniqueKeyFromHibernateSearchIndexName( hibernateSearchIndexName );
	}

	@Override
	public String extractUniqueKeyFromElasticsearchIndexName(String elasticsearchIndexName) {
		return delegate.extractUniqueKeyFromElasticsearchIndexName( elasticsearchIndexName );
	}
}
