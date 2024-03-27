/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.validation.impl.ElasticsearchPropertyMappingValidatorProvider;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

import com.google.gson.Gson;

public class ElasticsearchIndexSchemaManager implements IndexSchemaManager {

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaCreator schemaCreator;
	private final ElasticsearchSchemaDropper schemaDropper;
	private final ElasticsearchSchemaValidator schemaValidator;
	private final ElasticsearchSchemaMigrator schemaMigrator;
	private final ElasticsearchSchemaExporter schemaExporter;

	private final IndexNames indexNames;
	private final IndexMetadata expectedMetadata;
	private final ElasticsearchIndexLifecycleExecutionOptions executionOptions;
	private final Optional<String> backendName;

	public ElasticsearchIndexSchemaManager(Optional<String> backendName,
			Gson userFacingGson,
			ElasticsearchWorkFactory workFactory,
			ElasticsearchParallelWorkOrchestrator workOrchestrator,
			IndexLayoutStrategy indexLayoutStrategy,
			IndexNames indexNames, IndexMetadata expectedMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions,
			ElasticsearchPropertyMappingValidatorProvider propertyMappingValidatorProvider) {
		this.schemaAccessor = new ElasticsearchSchemaAccessor( workFactory, workOrchestrator );

		this.schemaCreator = new ElasticsearchSchemaCreator( schemaAccessor, indexLayoutStrategy );
		this.schemaDropper = new ElasticsearchSchemaDropper( schemaAccessor );
		this.schemaValidator = new ElasticsearchSchemaValidator( propertyMappingValidatorProvider );
		this.schemaMigrator = new ElasticsearchSchemaMigrator( schemaAccessor, schemaValidator );
		this.schemaExporter = new ElasticsearchSchemaExporter( userFacingGson, workFactory, indexLayoutStrategy );

		this.indexNames = indexNames;
		this.expectedMetadata = expectedMetadata;
		this.executionOptions = executionOptions;
		this.backendName = backendName;
	}

	@Override
	public CompletableFuture<?> createIfMissing(OperationSubmitter operationSubmitter) {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata, operationSubmitter )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions,
						operationSubmitter ) );
	}

	@Override
	public CompletableFuture<?> createOrValidate(ContextualFailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata, operationSubmitter )
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
						: schemaAccessor.waitForIndexStatus( indexNames, executionOptions, operationSubmitter )
				);
	}

	@Override
	public CompletableFuture<?> createOrUpdate(OperationSubmitter operationSubmitter) {
		return schemaCreator.createIndexIfAbsent( indexNames, expectedMetadata, operationSubmitter )
				.thenCompose( existingIndexMetadata -> {
					if ( existingIndexMetadata != null ) {
						return schemaMigrator.migrate(
								URLEncodedString.fromString( existingIndexMetadata.getPrimaryName() ),
								expectedMetadata, existingIndexMetadata.getMetadata(),
								operationSubmitter
						);
					}
					else {
						return CompletableFuture.completedFuture( null );
					}
				} )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions,
						operationSubmitter ) );
	}

	@Override
	public CompletableFuture<?> dropIfExisting(OperationSubmitter operationSubmitter) {
		return schemaDropper.dropIfExisting( indexNames, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> dropAndCreate(OperationSubmitter operationSubmitter) {
		return schemaDropper.dropIfExisting( indexNames, operationSubmitter )
				.thenCompose( ignored -> schemaCreator.createIndexAssumeNonExisting( indexNames, expectedMetadata,
						operationSubmitter ) )
				.thenCompose( ignored -> schemaAccessor.waitForIndexStatus( indexNames, executionOptions,
						operationSubmitter ) );
	}

	@Override
	public CompletableFuture<?> validate(ContextualFailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		return schemaAccessor.getCurrentIndexMetadata( indexNames, operationSubmitter )
				.thenAccept( actualIndexMetadata -> schemaValidator.validate(
						expectedMetadata, actualIndexMetadata.getMetadata(),
						failureCollector
				) )
				.thenCompose( ignored -> failureCollector.hasFailure()
						? CompletableFuture.completedFuture( null )
						: schemaAccessor.waitForIndexStatus( indexNames, executionOptions, operationSubmitter )
				);
	}

	@Override
	public void exportExpectedSchema(IndexSchemaCollector collector) {
		collector.indexSchema(
				backendName, indexNames.hibernateSearchIndex(), schemaExporter.export( expectedMetadata, indexNames ) );
	}
}
