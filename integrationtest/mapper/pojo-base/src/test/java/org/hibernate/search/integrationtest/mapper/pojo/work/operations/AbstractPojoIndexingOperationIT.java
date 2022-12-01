/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Abstract base for {@link AbstractPojoIndexingPlanOperationBaseIT}
 * and {@link AbstractPojoIndexerOperationBaseIT}
 */
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractPojoIndexingOperationIT {

	public static List<? extends Arguments> params() {
		List<Arguments> params = new ArrayList<>();
		MyRoutingBinder routingBinder = new MyRoutingBinder();
		for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
			for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
				params.add( Arguments.of( commitStrategy, refreshStrategy, null, null ) );
				params.add( Arguments.of( commitStrategy, refreshStrategy, null, routingBinder ) );
				params.add( Arguments.of( commitStrategy, refreshStrategy, "tenant1", null ) );
				params.add( Arguments.of( commitStrategy, refreshStrategy, "tenant1", routingBinder ) );
			}
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
	public String tenantId;
	public MyRoutingBinder routingBinder;

	protected SearchMapping mapping;

	@Mock
	private SelectionEntityLoader<IndexedEntity> indexedEntityLoaderMock;

	@Mock
	private SelectionEntityLoader<ContainedEntity> containedEntityLoaderMock;

	public void setup(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy, String tenantId, MyRoutingBinder routingBinder) {
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
		this.tenantId = tenantId;
		this.routingBinder = routingBinder;
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
							.selectionLoadingStrategy( (SelectionLoadingStrategy<IndexedEntity>)
									(includedTypes, options) -> indexedEntityLoaderMock ) );
					b.addEntityType( ContainedEntity.class, context -> context
							.selectionLoadingStrategy( (SelectionLoadingStrategy<ContainedEntity>)
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
				.commitStrategy( commitStrategy )
				.refreshStrategy( refreshStrategy )
				.tenantId( tenantId )
				.build();
	}

	protected final void expectIndexedEntityLoadingIfRelevant(Integer ... ids) {
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

	protected final void expectContainedEntityLoadingIfRelevant(Integer ... ids) {
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

	protected final void expectOperation(CompletableFuture<?> futureFromBackend, int id, String providedRoutingKey, String value) {
		expectOperation( futureFromBackend, ignored -> { }, id, providedRoutingKey, value );
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
		expectUpdateCausedByContained( futureFromBackend, ignored -> { }, id, value, containedValue );
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
