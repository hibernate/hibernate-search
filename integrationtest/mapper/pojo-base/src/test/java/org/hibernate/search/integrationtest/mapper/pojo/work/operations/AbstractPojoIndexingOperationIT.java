/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.PojoIndexingOperation.ADD;
import static org.hibernate.search.integrationtest.mapper.pojo.work.operations.PojoIndexingOperation.PURGE;
import static org.junit.Assume.assumeTrue;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Abstract base for {@link PojoIndexingPlanOperationIT}
 * and {@link PojoIndexerOperationIT}
 */
@RunWith(Parameterized.class)
public abstract class AbstractPojoIndexingOperationIT {

	@Parameterized.Parameters(name = "operation: {0}, commit: {1}, refresh: {2}, tenantID: {3}, routing: {4}")
	public static List<Object[]> parameters() {
		List<Object[]> params = new ArrayList<>();
		MyRoutingBinder routingBinder = new MyRoutingBinder();
		for ( PojoIndexingOperation operation : PojoIndexingOperation.values() ) {
			for ( DocumentCommitStrategy commitStrategy : DocumentCommitStrategy.values() ) {
				for ( DocumentRefreshStrategy refreshStrategy : DocumentRefreshStrategy.values() ) {
					params.add( new Object[] { operation, commitStrategy, refreshStrategy, null, null } );
					params.add( new Object[] { operation, commitStrategy, refreshStrategy, null, routingBinder } );
					params.add( new Object[] { operation, commitStrategy, refreshStrategy, "tenant1", null } );
					params.add( new Object[] { operation, commitStrategy, refreshStrategy, "tenant1", routingBinder } );
				}
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
	public PojoIndexingOperation operation;
	@Parameterized.Parameter(1)
	public DocumentCommitStrategy commitStrategy;
	@Parameterized.Parameter(2)
	public DocumentRefreshStrategy refreshStrategy;
	@Parameterized.Parameter(3)
	public String tenantId;
	@Parameterized.Parameter(4)
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

		MyRoutingBridge.indexed = true;
		MyRoutingBridge.previouslyIndexed = true;
		MyRoutingBridge.previousValues = null;
	}

	protected final boolean isAdd() {
		return ADD.equals( operation );
	}

	protected final boolean isPurge() {
		return PURGE.equals( operation );
	}

	protected final void assumeRoutingBridgeEnabled() {
		assumeTrue( "This test only makes sense when a routing bridge is configured and "
				+ "the operation takes the routing bridge into account",
				routingBinder != null && !isPurge() );
	}

	protected final SearchSession createSession() {
		return mapping.createSessionWithOptions()
				.commitStrategy( commitStrategy )
				.refreshStrategy( refreshStrategy )
				.tenantId( tenantId )
				.build();
	}

	protected final void expectOperation(CompletableFuture<?> futureFromBackend, int id, String providedRoutingKey, String value) {
		expectOperation( futureFromBackend, ignored -> { }, id, providedRoutingKey, value );
	}

	protected final void expectOperation(CompletableFuture<?> futureFromBackend,
			Consumer<BackendMock.DocumentWorkCallListContext> worksBefore,
			int id, String providedRoutingKey, String value) {
		BackendMock.DocumentWorkCallListContext context = backendMock.expectWorks(
				IndexedEntity.INDEX, commitStrategy, refreshStrategy
		);
		worksBefore.accept( context );
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
		operation.expect( context, tenantId, String.valueOf( id ), expectedRoutingKey, value );
		context.processedThenExecuted( futureFromBackend );
	}

}
