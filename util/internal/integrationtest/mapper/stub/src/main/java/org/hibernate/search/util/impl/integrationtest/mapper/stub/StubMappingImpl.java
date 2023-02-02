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

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.ToDocumentValueConvertContextImpl;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingStartContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.definition.spi.ProjectionRegistry;
import org.hibernate.search.engine.search.projection.spi.ProjectionMappedTypeContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;

public class StubMappingImpl implements StubMapping, MappingImplementor<StubMappingImpl> {

	private final Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier;
	private final StubMappingSchemaManagementStrategy schemaManagementStrategy;

	private final ToDocumentValueConvertContext toDocumentFieldValueConvertContext;

	private SearchIntegration.Handle integrationHandle;

	StubMappingFixture fixture = new StubMappingFixture( this );

	StubMappingImpl(Map<String, StubMappedIndex> mappedIndexesByTypeIdentifier,
			StubMappingSchemaManagementStrategy schemaManagementStrategy) {
		this.mappedIndexesByTypeIdentifier = mappedIndexesByTypeIdentifier;
		this.schemaManagementStrategy = schemaManagementStrategy;
		this.toDocumentFieldValueConvertContext = new ToDocumentValueConvertContextImpl( this );
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( SearchIntegration::close, integrationHandle, SearchIntegration.Handle::getOrNull );
			integrationHandle = null;
		}
	}

	@Override
	public BackendMappingHints hints() {
		return StubMappingHints.INSTANCE;
	}

	@Override
	public ProjectionMappedTypeContext mappedTypeContext(String mappedTypeName) {
		return fixture.typeContext( mappedTypeName );
	}

	@Override
	public SearchIntegration integration() {
		return integrationHandle.getOrFail();
	}

	@Override
	public StubMappingImpl toConcreteType() {
		return this;
	}

	@Override
	public final ToDocumentValueConvertContext toDocumentValueConvertContext() {
		return toDocumentFieldValueConvertContext;
	}

	@Override
	public ProjectionRegistry projectionRegistry() {
		return fixture.projectionRegistry;
	}

	@Override
	public StubSession session() {
		return new StubSession( this, null );
	}

	@Override
	public StubSession session(String tenantId) {
		return new StubSession( this, tenantId );
	}

	@Override
	public CompletableFuture<?> start(MappingStartContext context) {
		integrationHandle = context.integrationHandle();
		switch ( schemaManagementStrategy ) {
			case DROP_AND_CREATE_AND_DROP:
			case DROP_AND_CREATE_ON_STARTUP_ONLY:
				return doSchemaManagementOperation(
						indexSchemaManager -> indexSchemaManager.dropAndCreate( OperationSubmitter.blocking() ),
						context.failureCollector()
				);
			case DROP_ON_SHUTDOWN_ONLY:
			case NONE:
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
						indexSchemaManager -> indexSchemaManager.dropIfExisting( OperationSubmitter.blocking() ),
						context.failureCollector()
				);
			case DROP_AND_CREATE_ON_STARTUP_ONLY:
			case NONE:
			default:
				// Nothing to do
				return CompletableFuture.completedFuture( null );
		}
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	@Override
	public StubMappingFixture with() {
		return new StubMappingFixture( this );
	}

	private CompletableFuture<?> doSchemaManagementOperation(
			Function<IndexSchemaManager, CompletableFuture<?>> operation,
			ContextualFailureCollector failureCollector) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[mappedIndexesByTypeIdentifier.size()];
		int typeCounter = 0;

		for ( Map.Entry<String, StubMappedIndex> entry : mappedIndexesByTypeIdentifier.entrySet() ) {
			IndexSchemaManager delegate = entry.getValue().schemaManager();
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
