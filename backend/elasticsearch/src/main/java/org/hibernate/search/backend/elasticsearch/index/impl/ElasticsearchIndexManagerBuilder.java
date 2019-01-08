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
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> {

	// Exposed for tests
	public static final String TYPE_NAME = "typeName";

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final String elasticsearchIndexName;
	private final ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder;
	private final ElasticsearchIndexSettingsBuilder settingsBuilder;
	private final boolean refreshAfterWrite;

	public ElasticsearchIndexManagerBuilder(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, String elasticsearchIndexName,
			ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder,
			boolean refreshAfterWrite) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;

		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
		this.settingsBuilder = settingsBuilder;
		this.refreshAfterWrite = refreshAfterWrite;
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
		// TODO find out what to do with type names: what's the point if there is only one type per index anyway?
		URLEncodedString encodedTypeName = URLEncodedString.fromString( TYPE_NAME );

		ElasticsearchIndexModel model = schemaRootNodeBuilder
				.build( hibernateSearchIndexName, encodedElasticsearchIndexName, settingsBuilder );

		// create a stream orchestrator instance for each index,
		// it is supposed to be closed by the index manager we're building now
		ElasticsearchWorkOrchestrator streamOrchestrator = indexingBackendContext.createStreamOrchestrator( elasticsearchIndexName );

		// TODO make sure index initialization is performed in parallel for all indexes?
		indexingBackendContext.initializeIndex( streamOrchestrator, encodedElasticsearchIndexName, encodedTypeName, model )
				.join();

		return new ElasticsearchIndexManagerImpl( streamOrchestrator,
				indexingBackendContext, searchBackendContext,
				hibernateSearchIndexName, encodedElasticsearchIndexName,
				encodedTypeName, model,
				refreshAfterWrite
		);
	}

}
