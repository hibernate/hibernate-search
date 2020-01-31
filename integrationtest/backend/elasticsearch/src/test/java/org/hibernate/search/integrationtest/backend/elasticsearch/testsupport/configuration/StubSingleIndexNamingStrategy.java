/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy;

/**
 * A simple {@link org.hibernate.search.backend.elasticsearch.index.naming.IndexNamingStrategy}
 * that only supports one index.
 */
public class StubSingleIndexNamingStrategy implements IndexNamingStrategy {
	private final String writeAlias;
	private final String readAlias;

	public StubSingleIndexNamingStrategy(String writeAlias, String readAlias) {
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
