/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for dropping an index.
 * @author Gunnar Morling
 */
final class ElasticsearchSchemaDropper {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaDropper(ElasticsearchSchemaAccessor schemaAccessor) {
		this.schemaAccessor = schemaAccessor;
	}

	/**
	 * Drops an index, throwing an exception if dropping fails.
	 *
	 * <p>This method will skip operations silently if the index does not exist.
	 *
	 * @param indexNames The names of the index to drop.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	public CompletableFuture<?> dropIfExisting(IndexNames indexNames, OperationSubmitter operationSubmitter) {
		return schemaAccessor.getCurrentIndexMetadataOrNull( indexNames, operationSubmitter )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata == null ) {
						// Index does not exist: nothing to do.
						return CompletableFuture.completedFuture( null );
					}
					else {
						// Index exists: delete.
						// We need to use the primary name of the index: passing an alias to the drop-index call won't work.
						return schemaAccessor.dropIndexIfExisting(
								URLEncodedString.fromString( existingIndexMetadata.getPrimaryName() ),
								operationSubmitter
						);
					}
				} );
	}

}
