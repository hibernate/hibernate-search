/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaCreatorImpl implements ElasticsearchSchemaCreator {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaCreatorImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public CompletableFuture<?> createIndexAssumeNonExisting(IndexMetadata indexMetadata) {
		URLEncodedString indexName = indexMetadata.getName();

		return schemaAccessor.createIndexAssumeNonExisting(
				indexName, indexMetadata.getSettings(),
				indexMetadata.getMapping()
		);
	}

	@Override
	public CompletableFuture<IndexMetadata> createIndexIfAbsent(IndexMetadata indexMetadata) {
		URLEncodedString indexName = indexMetadata.getName();

		return schemaAccessor.getCurrentIndexMetadataOrNull( indexName )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return CompletableFuture.completedFuture( existingIndexMetadata );
					}
					else {
						return schemaAccessor.createIndexIgnoreExisting(
								indexName, indexMetadata.getSettings(),
								indexMetadata.getMapping()
						)
								.thenApply( ignored -> null );
					}
				} );
	}

}
