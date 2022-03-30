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
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

public class ElasticsearchIndexSchemaManager implements IndexSchemaManager {

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaCreator schemaCreator;
	private final ElasticsearchSchemaDropper schemaDropper;
	private final ElasticsearchSchemaValidator schemaValidator;
	private final ElasticsearchSchemaMigrator schemaMigrator;

	private final IndexNames indexNames;
	private final IndexMetadata expectedMetadata;
	private final ElasticsearchIndexLifecycleExecutionOptions executionOptions;

	public ElasticsearchIndexSchemaManager(ElasticsearchWorkBuilderFactory workBuilderFactory,
			ElasticsearchParallelWorkOrchestrator workOrchestrator,
			IndexLayoutStrategy indexLayoutStrategy,
			IndexNames indexNames, IndexMetadata expectedMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		this.schemaAccessor = new ElasticsearchSchemaAccessor( workBuilderFactory, workOrchestrator );

		this.schemaCreator = new ElasticsearchSchemaCreator( schemaAccessor, indexLayoutStrategy );
		this.schemaDropper = new ElasticsearchSchemaDropper( schemaAccessor );
		this.schemaValidator = new ElasticsearchSchemaValidator();
		this.schemaMigrator = new ElasticsearchSchemaMigrator( schemaAccessor, schemaValidator );

		this.indexNames = indexNames;
		this.expectedMetadata = expectedMetadata;
		this.executionOptions = executionOptions;
	}

	@Override
	public CompletableFuture<?> createIfMissing() {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions ) );
	}

	@Override
	public CompletableFuture<?> createOrValidate(ContextualFailureCollector failureCollector) {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata )
				.thenAccept( preExistingIndexMetadata -> {
					if ( preExistingIndexMetadata != null ) {
						schemaValidator.validate(
								expectedMetadata, preExistingIndexMetadata.getMetadata(),
								failureCollector
						);
					}
				} )
				.thenCompose( ignored -> failureCollector.hasFailure()
						? CompletableFuture.completedFuture( null )
						: schemaAccessor.waitForIndexStatus( indexNames, executionOptions )
				);
	}

	@Override
	public CompletableFuture<?> createOrUpdate() {
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

	@Override
	public CompletableFuture<?> dropIfExisting() {
		return schemaDropper.dropIfExisting( indexNames );
	}

	@Override
	public CompletableFuture<?> dropAndCreate() {
		return schemaDropper.dropIfExisting( indexNames )
				.thenCompose( ignored -> schemaCreator.createIndexAssumeNonExisting( indexNames, expectedMetadata ) )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions ) );
	}

	@Override
	public CompletableFuture<?> validate(ContextualFailureCollector failureCollector) {
		return schemaAccessor.getCurrentIndexMetadata( indexNames )
				.thenAccept( actualIndexMetadata -> schemaValidator.validate(
						expectedMetadata, actualIndexMetadata.getMetadata(),
						failureCollector
				) )
				.thenCompose( ignored -> failureCollector.hasFailure()
						? CompletableFuture.completedFuture( null )
						: schemaAccessor.waitForIndexStatus( indexNames, executionOptions )
				);
	}
}
