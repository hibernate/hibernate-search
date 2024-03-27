/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
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
import org.hibernate.search.mapper.pojo.standalone.loading.binding.EntityLoadingBinder;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Abstract base for {@link AbstractPojoIndexingPlanOperationBaseIT}
 * and {@link AbstractPojoIndexerOperationBaseIT}
 */
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
public abstract class AbstractPojoIndexingOperationIT {

	public static List<? extends Arguments> params() {
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

		List<Arguments> params = new ArrayList<>();
		MyRoutingBinder routingBinder = new MyRoutingBinder();
		for ( Object[] strategy : strategies ) {
			params.add( Arguments.of( strategy[0], strategy[1], null, null, strategy[2] ) );
			params.add( Arguments.of( strategy[0], strategy[1], null, routingBinder, strategy[2] ) );
			params.add( Arguments.of( strategy[0], strategy[1], "tenant1", null, strategy[2] ) );
			params.add( Arguments.of( strategy[0], strategy[1], "tenant1", routingBinder, strategy[2] ) );
		}
		return params;
	}

	@RegisterExtension
	public final BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public final StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	public DocumentCommitStrategy commitStrategy;
	public DocumentRefreshStrategy refreshStrategy;
	public Object tenantId;
	public MyRoutingBinder routingBinder;
	public IndexingPlanSynchronizationStrategy strategy;

	protected SearchMapping mapping;

	@Mock
	private SelectionEntityLoader<IndexedEntity> indexedEntityLoaderMock;

	@Mock
	private SelectionEntityLoader<ContainedEntity> containedEntityLoaderMock;

	public void setup(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId,
			MyRoutingBinder routingBinder, IndexingPlanSynchronizationStrategy strategy) {
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
		this.tenantId = tenantId;
		this.routingBinder = routingBinder;
		this.strategy = strategy;
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
					b.programmaticMapping().type( IndexedEntity.class )
							.searchEntity()
							.loadingBinder( (EntityLoadingBinder) context -> context
									.selectionLoadingStrategy( IndexedEntity.class,
											(includedTypes, options) -> indexedEntityLoaderMock ) );
					b.programmaticMapping().type( ContainedEntity.class )
							.searchEntity()
							.loadingBinder( (EntityLoadingBinder) context -> context
									.selectionLoadingStrategy( ContainedEntity.class,
											(includedTypes, options) -> containedEntityLoaderMock ) );
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
		assumeTrue(
				isImplicitRoutingEnabled(),
				"This test only makes sense when a routing bridge is configured and "
						+ "the operation takes the routing bridge into account"
		);
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
