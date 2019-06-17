/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;


public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> {

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final String elasticsearchIndexName;
	private final ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder;
	private final ElasticsearchIndexSettingsBuilder settingsBuilder;

	public ElasticsearchIndexManagerBuilder(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, String elasticsearchIndexName,
			ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;

		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
		this.settingsBuilder = settingsBuilder;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public ElasticsearchIndexManagerImpl build() {
		URLEncodedString encodedElasticsearchIndexName = URLEncodedString.fromString( elasticsearchIndexName );

		ElasticsearchIndexModel model = schemaRootNodeBuilder
				.build( hibernateSearchIndexName, encodedElasticsearchIndexName, settingsBuilder );

		return new ElasticsearchIndexManagerImpl(
				indexingBackendContext, searchBackendContext,
				hibernateSearchIndexName, encodedElasticsearchIndexName,
				model
		);
	}

}
