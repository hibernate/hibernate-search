/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;

/**
 * A simple {@link IndexLayoutStrategy}
 * that only supports one index.
 */
public class StubSingleIndexLayoutStrategy implements IndexLayoutStrategy {
	private final String writeAlias;
	private final String readAlias;

	public StubSingleIndexLayoutStrategy(String writeAlias, String readAlias) {
		this.writeAlias = writeAlias;
		this.readAlias = readAlias;
	}

	@Override
	public String createInitialElasticsearchIndexName(String hibernateSearchIndexName) {
		return defaultPrimaryName( hibernateSearchIndexName ).original;
	}

	@Override
	public String createWriteAlias(String hibernateSearchIndexName) {
		return writeAlias;
	}

	@Override
	public String createReadAlias(String hibernateSearchIndexName) {
		return readAlias;
	}
}
