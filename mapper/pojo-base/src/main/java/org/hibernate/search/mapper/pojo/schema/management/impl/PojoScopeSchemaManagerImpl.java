/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.schema.management.impl;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

public class PojoScopeSchemaManagerImpl implements PojoScopeSchemaManager {

	private final Set<? extends PojoSchemaManagementIndexedTypeContext> targetedTypeContexts;

	public PojoScopeSchemaManagerImpl(Set<? extends PojoSchemaManagementIndexedTypeContext> targetedTypeContexts) {
		this.targetedTypeContexts = targetedTypeContexts;
	}

	@Override
	public CompletableFuture<?> createIfMissing(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesBiFunction( IndexSchemaManager::createIfMissing, failureCollector, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> createOrValidate(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesTriFunction( IndexSchemaManager::createOrValidate, failureCollector, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> createOrUpdate(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesBiFunction( IndexSchemaManager::createOrUpdate, failureCollector, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> dropAndCreate(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesBiFunction(
				IndexSchemaManager::dropAndCreate,
				failureCollector,
				operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> dropIfExisting(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesBiFunction( IndexSchemaManager::dropIfExisting, failureCollector, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> validate(FailureCollector failureCollector, OperationSubmitter operationSubmitter) {
		return doOperationOnTypesTriFunction( IndexSchemaManager::validate, failureCollector, operationSubmitter );
	}

	@Override
	public void exportExpectedSchema(SearchSchemaCollector collector) {
		IndexSchemaCollectorDelegate collectorDelegate = new IndexSchemaCollectorDelegate( collector );
		for ( PojoSchemaManagementIndexedTypeContext typeContext : targetedTypeContexts ) {
			IndexSchemaManager delegate = typeContext.schemaManager();
			delegate.exportExpectedSchema( collectorDelegate );
		}
	}

	@Override
	public void exportExpectedSchema(Path targetDirectory) {
		exportExpectedSchema( new FileSearchSchemaCollector( targetDirectory ) );
	}

	private CompletableFuture<?> doOperationOnTypesBiFunction(
			BiFunction<IndexSchemaManager, OperationSubmitter, CompletableFuture<?>> operation,
			FailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		return doOperationOnTypesTriFunction(
				(schemaManager, typeFailureCollector, submitter) -> operation.apply( schemaManager, submitter ),
				failureCollector,
				operationSubmitter
		);
	}

	private CompletableFuture<?> doOperationOnTypesTriFunction(
			TriFunction<IndexSchemaManager, ContextualFailureCollector, OperationSubmitter, CompletableFuture<?>> operation,
			FailureCollector failureCollector,
			OperationSubmitter operationSubmitter) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[targetedTypeContexts.size()];
		int typeCounter = 0;

		for ( PojoSchemaManagementIndexedTypeContext typeContext : targetedTypeContexts ) {
			IndexSchemaManager delegate = typeContext.schemaManager();
			ContextualFailureCollector typeFailureCollector =
					failureCollector.withContext( PojoEventContexts.fromType( typeContext.typeIdentifier() ) );
			futures[typeCounter++] = operation.apply( delegate, typeFailureCollector, operationSubmitter )
					.exceptionally( Futures.handler( e -> {
						typeFailureCollector.add( Throwables.expectException( e ) );
						return null;
					} ) );
		}

		return CompletableFuture.allOf( futures );
	}

	private static class IndexSchemaCollectorDelegate implements IndexSchemaCollector {
		private final SearchSchemaCollector delegate;

		private IndexSchemaCollectorDelegate(SearchSchemaCollector delegate) {
			this.delegate = delegate;
		}

		@Override
		public void indexSchema(Optional<String> backendName, String indexName, SchemaExport export) {
			delegate.indexSchema( backendName, indexName, export );
		}
	}

}
