/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

/**
 * Abstract base for tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}
 * and {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexer}.
 */
@RunWith(Parameterized.class)
public abstract class AbstractPojoIndexingOperationIT {

	@Parameterized.Parameters(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	public static List<Object[]> parameters() {
		List<Object[]> params = new ArrayList<>();
		MyRoutingBinder routingBinder = new MyRoutingBinder();
		for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
			for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
				params.add( new Object[] { commitStrategy, refreshStrategy, null, null } );
				params.add( new Object[] { commitStrategy, refreshStrategy, null, routingBinder } );
				params.add( new Object[] { commitStrategy, refreshStrategy, "tenant1", null } );
				params.add( new Object[] { commitStrategy, refreshStrategy, "tenant1", routingBinder } );
			}
		}
		return params;
	}

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper =
			JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Parameterized.Parameter(0)
	public DocumentCommitStrategy commitStrategy;
	@Parameterized.Parameter(1)
	public DocumentRefreshStrategy refreshStrategy;
	@Parameterized.Parameter(2)
	public String tenantId;
	@Parameterized.Parameter(3)
	public MyRoutingBinder routingBinder;

	protected SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "value", String.class ) );

		mapping = setupHelper.start()
				.withConfiguration( b -> {
					if ( routingBinder != null ) {
						b.programmaticMapping().type( IndexedEntity.class )
								.indexed().routingBinder( routingBinder );
					}
				} )
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void indexer_success() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletableFuture<?> returnedFuture = execute( indexer, 1 );
			backendMock.verifyExpectationsMet();
			FutureAssert.assertThat( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			FutureAssert.assertThat( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void indexer_providedId() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 42, null, "1" );
			CompletableFuture<?> returnedFuture = execute( indexer, 42, 1 );
			backendMock.verifyExpectationsMet();
			FutureAssert.assertThat( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			FutureAssert.assertThat( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void indexer_providedId_providedRoutingKey() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 42, "UE-123", "1" );
			CompletableFuture<?> returnedFuture = execute( indexer, 42, "UE-123", 1 );
			backendMock.verifyExpectationsMet();
			FutureAssert.assertThat( returnedFuture ).isPending();

			futureFromBackend.complete( null );
			FutureAssert.assertThat( returnedFuture ).isSuccessful();
		}
	}

	@Test
	public void indexer_runtimeException() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		RuntimeException exception = new RuntimeException();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletableFuture<?> returnedFuture = execute( indexer, 1 );
			backendMock.verifyExpectationsMet();
			FutureAssert.assertThat( returnedFuture ).isPending();

			futureFromBackend.completeExceptionally( exception );
			FutureAssert.assertThat( returnedFuture ).isFailed( exception );
		}
	}

	@Test
	public void indexer_error() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		Error error = new Error();
		try ( SearchSession session = createSession() ) {
			SearchIndexer indexer = session.indexer();

			expectOperation( futureFromBackend, 1, null, "1" );
			CompletableFuture<?> returnedFuture = execute( indexer, 1 );
			backendMock.verifyExpectationsMet();
			FutureAssert.assertThat( returnedFuture ).isPending();

			futureFromBackend.completeExceptionally( error );
			FutureAssert.assertThat( returnedFuture ).isFailed( error );
		}
	}

	@Test
	public void indexingPlan_success() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation( futureFromBackend, 1, null, "1" );
			addTo( indexingPlan, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Test
	public void indexingPlan_providedId() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation( futureFromBackend, 42, null, "1" );
			addTo( indexingPlan, 42, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Test
	public void indexingPlan_providedId_providedRoutingKey() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectOperation( futureFromBackend, 42, "UE-123", "1" );
			addTo( indexingPlan, 42, "UE-123", 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Test
	public void indexingPlan_runtimeException() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		RuntimeException exception = new RuntimeException();
		Assertions.assertThatThrownBy( () -> {
			try ( SearchSession session = createSession() ) {
				SearchIndexingPlan indexingPlan = session.indexingPlan();
				expectOperation( futureFromBackend, 1, null, "1" );
				addTo( indexingPlan, 1 );
				// The session will wait for completion of the indexing plan upon closing,
				// so we need to complete it now.
				futureFromBackend.completeExceptionally( exception );
			}
		} )
				.isSameAs( exception );
	}

	@Test
	public void indexingPlan_error() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		Error error = new Error();
		Assertions.assertThatThrownBy( () -> {
			try ( SearchSession session = createSession() ) {
				SearchIndexingPlan indexingPlan = session.indexingPlan();
				expectOperation( futureFromBackend, 1, null, "1" );
				addTo( indexingPlan, 1 );
				// The session will wait for completion of the indexing plan upon closing,
				// so we need to complete it now.
				futureFromBackend.completeExceptionally( error );
			}
		} )
				.isSameAs( error );
	}

	protected boolean isPurge() {
		return false;
	}

	protected abstract void expectOperation(BackendMock.DocumentWorkCallListContext context, String tenantId,
			String id, String routingKey, String value);

	protected abstract void addTo(SearchIndexingPlan indexingPlan, int id);

	protected abstract void addTo(SearchIndexingPlan indexingPlan, Object providedId, int id);

	protected abstract void addTo(SearchIndexingPlan indexingPlan, Object providedId, String providedRoutingKey, int id);

	protected abstract CompletableFuture<?> execute(SearchIndexer indexer, int id);

	protected abstract CompletableFuture<?> execute(SearchIndexer indexer, Object providedId, int id);

	protected abstract CompletableFuture<?> execute(SearchIndexer indexer, Object providedId, String providedRoutingKey, int id);

	protected final IndexedEntity createEntity(int id) {
		IndexedEntity entity = new IndexedEntity();
		entity.setId( id );
		entity.setValue( String.valueOf( id ) );
		return entity;
	}

	protected final void addWorkInfo(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
	}

	protected final void addWorkInfoAndDocument(StubDocumentWork.Builder builder, String tenantId,
			String identifier, String routingKey, String value) {
		builder.tenantIdentifier( tenantId );
		builder.identifier( identifier );
		builder.routingKey( routingKey );
		builder.document( StubDocumentNode.document().field( "value", value ).build() );
	}

	private SearchSession createSession() {
		return mapping.createSessionWithOptions()
				.commitStrategy( commitStrategy )
				.refreshStrategy( refreshStrategy )
				.tenantId( tenantId )
				.build();
	}

	private void expectOperation(CompletableFuture<?> futureFromBackend, int id, String providedRoutingKey, String value) {
		BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks(
				IndexedEntity.INDEX, commitStrategy, refreshStrategy
		);
		String expectedRoutingKey;
		if ( providedRoutingKey != null ) {
			expectedRoutingKey = providedRoutingKey;
		}
		// We don't apply the routing bridge to purge() operations
		else if ( routingBinder != null && !isPurge() ) {
			expectedRoutingKey = MyRoutingBridge.toRoutingKey( tenantId, id, value );
		}
		else {
			expectedRoutingKey = null;
		}
		expectOperation( context, tenantId, String.valueOf( id ), expectedRoutingKey, value );
		context.processedThenExecuted( futureFromBackend );
	}

	@Indexed(index = PojoIndexingPlanBaseIT.IndexedEntity.INDEX)
	public static final class IndexedEntity {

		public static final String INDEX = "IndexedEntity";

		private Integer id;

		private String value;

		@DocumentId
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@GenericField
		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public static final class MyRoutingBinder implements RoutingBinder {
		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

		@Override
		public void bind(RoutingBindingContext context) {
			context.bridge( IndexedEntity.class, new MyRoutingBridge() );
		}
	}

	private static final class MyRoutingBridge implements RoutingBridge<IndexedEntity> {
		public static String toRoutingKey(String tenantIdentifier, Object entityIdentifier, String value) {
			StringBuilder keyBuilder = new StringBuilder();
			if ( tenantIdentifier != null ) {
				keyBuilder.append( tenantIdentifier ).append( "/" );
			}
			keyBuilder.append( entityIdentifier ).append( "/" );
			keyBuilder.append( value );
			return keyBuilder.toString();
		}

		@Override
		public void route(DocumentRoutes routes, Object entityIdentifier, IndexedEntity indexedEntity,
				RoutingBridgeRouteContext context) {
			String tenantIdentifier = context.tenantIdentifier();
			routes.addRoute()
					.routingKey( toRoutingKey( tenantIdentifier, entityIdentifier, indexedEntity.value ) );
		}
	}

}
