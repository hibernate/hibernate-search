/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> {

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String hibernateSearchIndexName;
	private final String elasticsearchIndexName;
	private final ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder;

	private final BackendBuildContext buildContext;
	private final ConfigurationPropertySource propertySource;

	public ElasticsearchIndexManagerBuilder(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String hibernateSearchIndexName, String elasticsearchIndexName,
			ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder,
			BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;

		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;

		this.buildContext = buildContext;
		this.propertySource = propertySource;
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
		URLEncodedString encodedTypeName = URLEncodedString.fromString( "typeName" );

		ElasticsearchIndexModel model = new ElasticsearchIndexModel(
				hibernateSearchIndexName, encodedElasticsearchIndexName,
				schemaRootNodeBuilder
		);

		// TODO make sure index initialization is performed in parallel for all indexes?
		indexingBackendContext.initializeIndex( encodedElasticsearchIndexName, encodedTypeName, model )
				.join();

		return new ElasticsearchIndexManagerImpl(
				indexingBackendContext, searchBackendContext,
				hibernateSearchIndexName, encodedElasticsearchIndexName,
				encodedTypeName, model
		);
	}

}
