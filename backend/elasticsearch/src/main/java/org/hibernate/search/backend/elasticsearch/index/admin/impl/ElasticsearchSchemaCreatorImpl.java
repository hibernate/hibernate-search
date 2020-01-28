/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaCreatorImpl implements ElasticsearchSchemaCreator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaCreatorImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public CompletableFuture<?> createIndexAssumeNonExisting(IndexMetadata indexMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		return schemaAccessor.createIndexAssumeNonExisting(
				indexName, indexMetadata.getSettings(),
				indexMetadata.getMapping()
		)
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexName, executionOptions ) );
	}

	@Override
	public CompletableFuture<Boolean> createIndexIfAbsent(IndexMetadata indexMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		return schemaAccessor.indexExists( indexName )
				.thenCompose( exists -> {
					if ( exists ) {
						return CompletableFuture.completedFuture( false );
					}
					else {
						return schemaAccessor.createIndexIgnoreExisting(
								indexName, indexMetadata.getSettings(),
								indexMetadata.getMapping()
						);
					}
				} )
				.thenCompose( created -> schemaAccessor.waitForIndexStatus( indexName, executionOptions )
							.thenApply( ignored -> created ) );
	}

	@Override
	public CompletableFuture<?> checkIndexExists(URLEncodedString indexName, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		return schemaAccessor.indexExists( indexName )
				.thenCompose( exists -> {
					if ( exists ) {
						return schemaAccessor.waitForIndexStatus( indexName, executionOptions );
					}
					else {
						throw log.indexMissing( indexName );
					}
				} );
	}

}
