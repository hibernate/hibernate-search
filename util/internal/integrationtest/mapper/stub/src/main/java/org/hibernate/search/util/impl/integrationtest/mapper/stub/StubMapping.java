/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Futures;

public class StubMapping implements MappingImplementor<StubMapping> {

	private final Map<String, StubMappingIndexManager> indexManagersByTypeIdentifier;

	private final StubMappingSchemaManagementStrategy schemaManagementStrategy;

	StubMapping(Map<String, StubMappingIndexManager> indexManagersByTypeIdentifier,
			StubMappingSchemaManagementStrategy schemaManagementStrategy) {
		this.indexManagersByTypeIdentifier = indexManagersByTypeIdentifier;
		this.schemaManagementStrategy = schemaManagementStrategy;
	}

	@Override
	public StubMapping toConcreteType() {
		return this;
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
		switch ( schemaManagementStrategy ) {
			case DROP_AND_CREATE_AND_DROP:
			case DROP_AND_CREATE_ON_STARTUP_ONLY:
				return doSchemaManagementOperation(
						IndexSchemaManager::dropAndCreate,
						context.getFailureCollector()
				);
			case DROP_ON_SHUTDOWN_ONLY:
			default:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
		}
	}

	@Override
	public CompletableFuture<?> preStop(MappingPreStopContext context) {
		switch ( schemaManagementStrategy ) {
			case DROP_AND_CREATE_AND_DROP:
			case DROP_ON_SHUTDOWN_ONLY:
				return doSchemaManagementOperation(
						IndexSchemaManager::dropIfExisting,
						context.getFailureCollector()
				);
			case DROP_AND_CREATE_ON_STARTUP_ONLY:
			default:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
		}
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	public StubMappingIndexManager getIndexMappingByTypeIdentifier(String typeId) {
		return indexManagersByTypeIdentifier.get( typeId );
	}

	private CompletableFuture<?> doSchemaManagementOperation(
			Function<IndexSchemaManager, CompletableFuture<?>> operation,
			ContextualFailureCollector failureCollector) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[indexManagersByTypeIdentifier.size()];
		int typeCounter = 0;

		for ( Map.Entry<String, StubMappingIndexManager> entry : indexManagersByTypeIdentifier.entrySet() ) {
			IndexSchemaManager delegate = entry.getValue().getSchemaManager();
			ContextualFailureCollector typeFailureCollector =
					failureCollector.withContext( EventContexts.fromType( entry.getKey() ) );
			futures[typeCounter++] = operation.apply( delegate )
					.exceptionally( Futures.handler( e -> {
						typeFailureCollector.add( e );
						return null;
					} ) );
		}

		return CompletableFuture.allOf( futures );
	}
}
