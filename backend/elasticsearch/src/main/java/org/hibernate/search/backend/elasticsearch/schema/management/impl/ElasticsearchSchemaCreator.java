/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for creating an index and its mappings based on provided metadata.
 * @author Gunnar Morling
 */
final class ElasticsearchSchemaCreator {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	private final IndexLayoutStrategy indexLayoutStrategy;

	public ElasticsearchSchemaCreator(ElasticsearchSchemaAccessor schemaAccessor,
			IndexLayoutStrategy indexLayoutStrategy) {
		this.schemaAccessor = schemaAccessor;
		this.indexLayoutStrategy = indexLayoutStrategy;
	}

	/**
	 * Create an index and its mapping.
	 *
	 * @param indexNames The index names.
	 * @param indexMetadata The expected index metadata.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	public CompletableFuture<?> createIndexAssumeNonExisting(IndexNames indexNames, IndexMetadata indexMetadata,
			OperationSubmitter operationSubmitter) {
		return schemaAccessor.createIndexAssumeNonExisting(
				createPrimaryIndexName( indexNames ),
				indexMetadata.getAliases(),
				indexMetadata.getSettings(),
				indexMetadata.getMapping(),
				operationSubmitter
		);
	}

	/**
	 * Create an index and its mapping, but only if the index doesn't already exist.
	 *
	 * @param indexNames The index names.
	 * @param indexMetadata The expected index metadata.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future holding the metadata of the pre-existing index, or null if the index had to be created.
	 * @throws SearchException If an error occurs.
	 */
	public CompletableFuture<ExistingIndexMetadata> createIndexIfAbsent(IndexNames indexNames, IndexMetadata indexMetadata,
			OperationSubmitter operationSubmitter) {
		return schemaAccessor.getCurrentIndexMetadataOrNull( indexNames, operationSubmitter )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return CompletableFuture.completedFuture( existingIndexMetadata );
					}
					else {
						return schemaAccessor.createIndexIgnoreExisting(
								createPrimaryIndexName( indexNames ),
								indexMetadata.getAliases(),
								indexMetadata.getSettings(),
								indexMetadata.getMapping(),
								operationSubmitter
						)
								.thenApply( ignored -> null );
					}
				} );
	}

	private URLEncodedString createPrimaryIndexName(IndexNames indexNames) {
		return IndexNames.encodeName(
				indexLayoutStrategy.createInitialElasticsearchIndexName( indexNames.hibernateSearchIndex() )
		);
	}

}
