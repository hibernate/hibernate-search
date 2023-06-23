/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * Abstract base for {@link AbstractPojoIndexingPlanOperationBaseIT}
 * and {@link AbstractPojoIndexerOperationBaseIT}
 */
@RunWith(Parameterized.class)
public abstract class AbstractPojoIndexingOperationIT {

	@Parameterized.Parameters(name = "commit: {0}, refresh: {1}, tenantID: {2}, routing: {3}")
	public static List<Object[]> parameters() {
		Object[][] strategies = new Object[][] {
				new Object[] {
						DocumentCommitStrategy.NONE,
						DocumentRefreshStrategy.NONE,
						IndexingPlanSynchronizationStrategy.async() },
				new Object[] {
						DocumentCommitStrategy.FORCE,
						DocumentRefreshStrategy.NONE,
						IndexingPlanSynchronizationStrategy.writeSync() },
				new Object[] {
						DocumentCommitStrategy.NONE,
						DocumentRefreshStrategy.FORCE,
						IndexingPlanSynchronizationStrategy.readSync() },
				new Object[] {
						DocumentCommitStrategy.FORCE,
						DocumentRefreshStrategy.FORCE,
						IndexingPlanSynchronizationStrategy.sync() }
		};

		List<Object[]> params = new ArrayList<>();
		MyRoutingBinder routingBinder = new MyRoutingBinder();
		for ( Object[] strategy : strategies ) {
			params.add( new Object[] { strategy[0], strategy[1], null, null, strategy[2] } );
			params.add( new Object[] { strategy[0], strategy[1], null, routingBinder, strategy[2] } );
			params.add( new Object[] { strategy[0], strategy[1], "tenant1", null, strategy[2] } );
			params.add( new Object[] { strategy[0], strategy[1], "tenant1", routingBinder, strategy[2] } );
		}
		return params;
	}

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Rule
	public final MockitoRule mockito = MockitoJUnit.rule().strictness( Strictness.STRICT_STUBS );

	@Parameterized.Parameter
	public DocumentCommitStrategy commitStrategy;
	@Parameterized.Parameter(1)
	public DocumentRefreshStrategy refreshStrategy;
	@Parameterized.Parameter(2)
	public String tenantId;
	@Parameterized.Parameter(3)
	public MyRoutingBinder routingBinder;
	@Parameterized.Parameter(4)
	public IndexingPlanSynchronizationStrategy strategy;

	protected SearchMapping mapping;

	@Mock
	private SelectionEntityLoader<IndexedEntity> indexedEntityLoaderMock;

	@Mock
	private SelectionEntityLoader<ContainedEntity> containedEntityLoaderMock;

	@Before
	public void setup() {
		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "value", String.class )
				.objectField( "contained", b2 -> b2
						.field( "value", String.class ) ) );

		mapping = setupHelper.start()
				.withConfiguration( b -> {
					if ( routingBinder != null ) {
						b.programmaticMapping().type( IndexedEntity.class )
								.indexed().routingBinder( routingBinder );
					}
					b.addEntityType( IndexedEntity.class, context -> context
							.selectionLoadingStrategy( (SelectionLoadingStrategy<
									IndexedEntity>) (includedTypes, options) -> indexedEntityLoaderMock ) );
					b.addEntityType( ContainedEntity.class, context -> context
							.selectionLoadingStrategy( (SelectionLoadingStrategy<
									ContainedEntity>) (includedTypes, options) -> containedEntityLoaderMock ) );
				} )
				.setup( IndexedEntity.class );

		backendMock.verifyExpectationsMet();

		MyRoutingBridge.indexed = true;
		MyRoutingBridge.previouslyIndexed = true;
		MyRoutingBridge.previousValues = null;
	}

	protected abstract PojoIndexingOperationScenario scenario();

	protected final boolean isAdd() {
		return BackendIndexingOperation.ADD.equals( scenario().expectedBackendOperation );
	}

	protected final boolean isDelete() {
		return BackendIndexingOperation.DELETE.equals( scenario().expectedBackendOperation );
	}

	protected abstract boolean isImplicitRoutingEnabled();

	protected final void assumeImplicitRoutingEnabled() {
		assumeTrue( "This test only makes sense when a routing bridge is configured and "
				+ "the operation takes the routing bridge into account",
				isImplicitRoutingEnabled() );
	}

	protected final SearchSession createSession() {
		return mapping.createSessionWithOptions()
				.indexingPlanSynchronizationStrategy( strategy )
				.tenantId( tenantId )
				.build();
	}

	protected final void expectIndexedEntityLoadingIfRelevant(Integer... ids) {
		List<IndexedEntity> entities = new ArrayList<>();
		for ( Integer id : ids ) {
			entities.add( IndexedEntity.of( id ) );
		}
		expectIndexedEntityLoadingIfRelevant( Arrays.asList( ids ), entities );
	}

	protected final void expectIndexedEntityLoadingIfRelevant(List<Integer> ids, List<IndexedEntity> entities) {
		if ( !scenario().expectImplicitLoadingOnNullEntity() ) {
			return;
		}
		if ( !scenario().isEntityPresentOnLoading() ) {
			entities = new ArrayList<>( entities );
			Collections.fill( entities, null );
		}
		when( indexedEntityLoaderMock.load( ids, null ) ).thenReturn( entities );
	}

	protected final void expectContainedEntityLoadingIfRelevant(Integer... ids) {
		List<ContainedEntity> entities = new ArrayList<>();
		for ( Integer id : ids ) {
			entities.add( ContainedEntity.of( id ) );
		}
		expectContainedEntityLoadingIfRelevant( Arrays.asList( ids ), entities );
	}

	protected final void expectContainedEntityLoadingIfRelevant(List<Integer> ids, List<ContainedEntity> entities) {
		if ( !scenario().expectImplicitLoadingOnNullEntity() ) {
			return;
		}
		if ( !scenario().isEntityPresentOnLoading() ) {
			entities = new ArrayList<>( entities );
			Collections.fill( entities, null );
		}
		when( containedEntityLoaderMock.load( ids, null ) ).thenReturn( entities );
	}

	protected final void expectOperation(CompletableFuture<?> futureFromBackend, int id, String providedRoutingKey,
			String value) {
		expectOperation( futureFromBackend, ignored -> {}, id, providedRoutingKey, value );
	}

	protected final void expectOperation(CompletableFuture<?> futureFromBackend,
			Consumer<BackendMock.DocumentWorkCallListContext> worksBefore,
			int id, String providedRoutingKey, String value) {
		BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks(
				IndexedEntity.INDEX, commitStrategy, refreshStrategy
		)
				.createAndExecuteFollowingWorks( futureFromBackend );
		worksBefore.accept( context );
		String expectedRoutingKey;
		if ( isImplicitRoutingEnabled() ) {
			expectedRoutingKey = MyRoutingBridge.toRoutingKey( tenantId, id, value );
		}
		else {
			expectedRoutingKey = providedRoutingKey;
		}
		scenario().expectedBackendOperation.expect( context,
				tenantId, String.valueOf( id ), expectedRoutingKey, value, null );
	}

	protected final void expectNoOperation(CompletableFuture<?> futureFromBackend,
			Consumer<BackendMock.DocumentWorkCallListContext> worksBefore,
			int id, String providedRoutingKey, String value) {
		BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks(
				IndexedEntity.INDEX, commitStrategy, refreshStrategy
		)
				.createAndExecuteFollowingWorks( futureFromBackend );
		worksBefore.accept( context );
		String expectedRoutingKey;
		if ( isImplicitRoutingEnabled() ) {
			expectedRoutingKey = MyRoutingBridge.toRoutingKey( tenantId, id, value );
		}
		else {
			expectedRoutingKey = providedRoutingKey;
		}
		scenario().expectedBackendOperation.expect( context,
				tenantId, String.valueOf( id ), expectedRoutingKey, value, null );
	}

	protected final void expectUpdateCausedByContained(CompletableFuture<?> futureFromBackend, int id,
			String value, String containedValue) {
		expectUpdateCausedByContained( futureFromBackend, ignored -> {}, id, value, containedValue );
	}

	protected final void expectUpdateCausedByContained(CompletableFuture<?> futureFromBackend,
			Consumer<BackendMock.DocumentWorkCallListContext> worksBefore,
			int id, String value, String containedValue) {
		BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks(
				IndexedEntity.INDEX, commitStrategy, refreshStrategy
		);
		worksBefore.accept( context );
		String expectedRoutingKey;
		if ( isImplicitRoutingEnabled() ) {
			expectedRoutingKey = MyRoutingBridge.toRoutingKey( tenantId, id, value );
		}
		else {
			expectedRoutingKey = null;
		}
		BackendIndexingOperation.ADD_OR_UPDATE.expect( context.createAndExecuteFollowingWorks( futureFromBackend ),
				tenantId, String.valueOf( id ), expectedRoutingKey, value, containedValue );
	}

}
