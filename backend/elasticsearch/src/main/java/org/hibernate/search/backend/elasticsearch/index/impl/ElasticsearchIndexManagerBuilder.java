/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexRootBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;

public class ElasticsearchIndexManagerBuilder implements IndexManagerBuilder {

	private final IndexManagerBackendContext backendContext;

	private final ElasticsearchIndexRootBuilder schemaRootNodeBuilder;
	private final List<DocumentMetadataContributor> documentMetadataContributors;

	public ElasticsearchIndexManagerBuilder(IndexManagerBackendContext backendContext,
			ElasticsearchIndexRootBuilder schemaRootNodeBuilder,
			List<DocumentMetadataContributor> documentMetadataContributors) {
		this.backendContext = backendContext;

		this.schemaRootNodeBuilder = schemaRootNodeBuilder;
		this.documentMetadataContributors = documentMetadataContributors;
	}

	@Override
	public void closeOnFailure() {
		// Nothing to do
	}

	@Override
	public IndexRootBuilder schemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public ElasticsearchIndexManagerImpl build() {
		ElasticsearchIndexModel model = schemaRootNodeBuilder.build();

		return new ElasticsearchIndexManagerImpl(
				backendContext,
				model,
				documentMetadataContributors
		);
	}

}
