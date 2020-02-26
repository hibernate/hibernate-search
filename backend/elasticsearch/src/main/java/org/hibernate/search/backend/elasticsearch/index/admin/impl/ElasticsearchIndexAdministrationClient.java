/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchEventContexts;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
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

	private final IndexNames indexNames;
	private final IndexMetadata expectedMetadata;
	private final ElasticsearchIndexLifecycleExecutionOptions executionOptions;

	public ElasticsearchIndexAdministrationClient(ElasticsearchWorkBuilderFactory workBuilderFactory,
			ElasticsearchWorkOrchestrator workOrchestrator,
			IndexLayoutStrategy indexLayoutStrategy,
			IndexNames indexNames, IndexMetadata expectedMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		this.schemaAccessor = new ElasticsearchSchemaAccessor( workBuilderFactory, workOrchestrator );

		this.schemaCreator = new ElasticsearchSchemaCreatorImpl( schemaAccessor, indexLayoutStrategy );
		this.schemaDropper = new ElasticsearchSchemaDropperImpl( schemaAccessor );
		this.schemaValidator = new ElasticsearchSchemaValidatorImpl();
		this.schemaMigrator = new ElasticsearchSchemaMigratorImpl( schemaAccessor, schemaValidator );

		this.indexNames = indexNames;
		this.expectedMetadata = expectedMetadata;
		this.executionOptions = executionOptions;
	}

	public CompletableFuture<?> createIfAbsent() {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions ) );
	}

	public CompletableFuture<?> dropAndCreate() {
		return schemaDropper.dropIfExisting( indexNames )
				.thenCompose( ignored -> schemaCreator.createIndexAssumeNonExisting( indexNames, expectedMetadata ) )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions ) );
	}

	public CompletableFuture<?> dropIfExisting() {
		return schemaDropper.dropIfExisting( indexNames );
	}

	public CompletableFuture<?> update() {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return schemaMigrator.migrate(
								URLEncodedString.fromString( existingIndexMetadata.getPrimaryName() ),
								expectedMetadata, existingIndexMetadata.getMetadata()
						);
					}
					else {
						return CompletableFuture.completedFuture( null );
					}
				} )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions ) );
	}

	public CompletableFuture<?> validate(ContextualFailureCollector failureCollector) {
		return schemaAccessor.getCurrentIndexMetadata( indexNames )
				.thenAccept( actualIndexMetadata ->
						schemaValidator.validate(
								expectedMetadata, actualIndexMetadata.getMetadata(),
								failureCollector.withContext( ElasticsearchEventContexts.getSchemaValidation() )
						)
				)
				.thenCompose( ignored -> failureCollector.hasFailure()
						? CompletableFuture.completedFuture( null )
						: schemaAccessor.waitForIndexStatus( indexNames, executionOptions )
				);
	}
}
