/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

/**
 * The default {@link ElasticsearchSchemaDropper} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaDropperImpl implements ElasticsearchSchemaDropper {

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaDropperImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public CompletableFuture<?> dropIfExisting(IndexNames indexNames) {
		return schemaAccessor.getCurrentIndexMetadataOrNull( indexNames )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata == null ) {
						// Index does not exist: nothing to do.
						return CompletableFuture.completedFuture( null );
					}
					else {
						// Index exists: delete.
						// We need to use the primary name of the index: passing an alias to the drop-index call won't work.
						return schemaAccessor.dropIndexIfExisting(
								URLEncodedString.fromString( existingIndexMetadata.getPrimaryName() )
						);
					}
				} );
	}

}
