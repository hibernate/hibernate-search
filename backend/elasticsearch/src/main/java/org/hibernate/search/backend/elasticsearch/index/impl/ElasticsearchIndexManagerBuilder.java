/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;


public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> {

	private final IndexManagerBackendContext backendContext;

	private final String hibernateSearchIndexName;
	private final String elasticsearchIndexName;
	private final ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder;
	private final ElasticsearchIndexSettingsBuilder settingsBuilder;
	private final List<DocumentMetadataContributor> documentMetadataContributors;

	public ElasticsearchIndexManagerBuilder(IndexManagerBackendContext backendContext,
			String hibernateSearchIndexName, String elasticsearchIndexName,
			ElasticsearchIndexSchemaRootNodeBuilder schemaRootNodeBuilder,
			ElasticsearchIndexSettingsBuilder settingsBuilder,
			List<DocumentMetadataContributor> documentMetadataContributors) {
		this.backendContext = backendContext;

		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
		this.settingsBuilder = settingsBuilder;
		this.documentMetadataContributors = documentMetadataContributors;
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
				backendContext,
				hibernateSearchIndexName, encodedElasticsearchIndexName,
				model,
				documentMetadataContributors
		);
	}

}
