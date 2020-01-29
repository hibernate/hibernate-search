/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

/**
 * The administration client for a given Elasticsearch index.
 * <p>
 * This interface is split from the rest of the code because we may one day expose these operations to users
 * (so that they can manage index updates more finely).
 */
public class ElasticsearchIndexAdministrationClient {

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaCreator schemaCreator;
	private final ElasticsearchSchemaDropper schemaDropper;
	private final ElasticsearchSchemaValidator schemaValidator;
	private final ElasticsearchSchemaMigrator schemaMigrator;

	private final IndexMetadata expectedMetadata;

	public ElasticsearchIndexAdministrationClient(ElasticsearchLink link,
			ElasticsearchWorkOrchestrator workOrchestrator,
			IndexMetadata expectedMetadata) {
		this.schemaAccessor = new ElasticsearchSchemaAccessor( link, workOrchestrator );

		this.schemaCreator = new ElasticsearchSchemaCreatorImpl( schemaAccessor );
		this.schemaDropper = new ElasticsearchSchemaDropperImpl( schemaAccessor );
		this.schemaValidator = new ElasticsearchSchemaValidatorImpl();
		this.schemaMigrator = new ElasticsearchSchemaMigratorImpl( schemaAccessor, schemaValidator );

		this.expectedMetadata = expectedMetadata;
	}

	public CompletableFuture<?> createIfAbsent(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		return schemaCreator.createIndexIfAbsent( expectedMetadata )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( expectedMetadata.getName(), executionOptions ) );
	}

	public CompletableFuture<?> dropAndCreate(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		return schemaDropper.dropIfExisting( expectedMetadata.getName() )
				.thenCompose( ignored -> schemaCreator.createIndexAssumeNonExisting( expectedMetadata ) )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( expectedMetadata.getName(), executionOptions ) );
	}

	public CompletableFuture<?> dropIfExisting(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		return schemaDropper.dropIfExisting( expectedMetadata.getName() );
	}

	public CompletableFuture<?> update(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		return schemaCreator.createIndexIfAbsent( expectedMetadata )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return schemaMigrator.migrate( expectedMetadata.getName(), expectedMetadata, existingIndexMetadata );
					}
					else {
						return CompletableFuture.completedFuture( null );
					}
				} )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( expectedMetadata.getName(), executionOptions ) );
	}

	public CompletableFuture<?> validate(ElasticsearchIndexLifecycleExecutionOptions executionOptions,
			ContextualFailureCollector failureCollector) {
		return schemaAccessor.getCurrentIndexMetadata( expectedMetadata.getName() )
				.thenAccept( actualIndexMetadata ->
						schemaValidator.validate( expectedMetadata, actualIndexMetadata, failureCollector )
				)
				.thenCompose( ignored -> failureCollector.hasFailure()
						? CompletableFuture.completedFuture( null )
						: schemaAccessor.waitForIndexStatus( expectedMetadata.getName(), executionOptions )
				);
	}
}
