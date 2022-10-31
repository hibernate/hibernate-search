/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.schema.management.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
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

}
