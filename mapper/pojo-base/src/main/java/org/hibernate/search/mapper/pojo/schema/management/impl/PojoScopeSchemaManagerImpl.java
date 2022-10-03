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
import java.util.function.Function;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

public class PojoScopeSchemaManagerImpl implements PojoScopeSchemaManager {

	private final Set<? extends PojoSchemaManagementIndexedTypeContext> targetedTypeContexts;

	public PojoScopeSchemaManagerImpl(Set<? extends PojoSchemaManagementIndexedTypeContext> targetedTypeContexts) {
		this.targetedTypeContexts = targetedTypeContexts;
	}

	@Override
	public CompletableFuture<?> createIfMissing(FailureCollector failureCollector) {
		return doOperationOnTypes( IndexSchemaManager::createIfMissing, failureCollector );
	}

	@Override
	public CompletableFuture<?> createOrValidate(FailureCollector failureCollector) {
		return doOperationOnTypes( IndexSchemaManager::createOrValidate, failureCollector );
	}

	@Override
	public CompletableFuture<?> createOrUpdate(FailureCollector failureCollector) {
		return doOperationOnTypes( IndexSchemaManager::createOrUpdate, failureCollector );
	}

	@Override
	public CompletableFuture<?> dropAndCreate(FailureCollector failureCollector) {
		return doOperationOnTypes(
				IndexSchemaManager::dropAndCreate,
				failureCollector
		);
	}

	@Override
	public CompletableFuture<?> dropIfExisting(FailureCollector failureCollector) {
		return doOperationOnTypes( IndexSchemaManager::dropIfExisting, failureCollector );
	}

	@Override
	public CompletableFuture<?> validate(FailureCollector failureCollector) {
		return doOperationOnTypes( IndexSchemaManager::validate, failureCollector );
	}

	private CompletableFuture<?> doOperationOnTypes(
			Function<IndexSchemaManager, CompletableFuture<?>> operation,
			FailureCollector failureCollector) {
		return doOperationOnTypes(
				(schemaManager, typeFailureCollector) -> operation.apply( schemaManager ),
				failureCollector
		);
	}

	private CompletableFuture<?> doOperationOnTypes(
			BiFunction<IndexSchemaManager, ContextualFailureCollector, CompletableFuture<?>> operation,
			FailureCollector failureCollector) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[targetedTypeContexts.size()];
		int typeCounter = 0;

		for ( PojoSchemaManagementIndexedTypeContext typeContext : targetedTypeContexts ) {
			IndexSchemaManager delegate = typeContext.schemaManager();
			ContextualFailureCollector typeFailureCollector =
					failureCollector.withContext( PojoEventContexts.fromType( typeContext.typeIdentifier() ) );
			futures[typeCounter++] = operation.apply( delegate, typeFailureCollector )
					.exceptionally( Futures.handler( e -> {
						typeFailureCollector.add( Throwables.expectException( e ) );
						return null;
					} ) );
		}

		return CompletableFuture.allOf( futures );
	}
}
