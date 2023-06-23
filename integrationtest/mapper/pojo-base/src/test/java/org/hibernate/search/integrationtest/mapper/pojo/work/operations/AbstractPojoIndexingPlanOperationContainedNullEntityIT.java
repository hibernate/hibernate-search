/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work.operations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Test;

/**
 * Tests of individual operations in {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}
 * when the contained entity passed to the operation is null.
 */
@TestForIssue(jiraKey = "HSEARCH-4141")
public abstract class AbstractPojoIndexingPlanOperationContainedNullEntityIT extends AbstractPojoIndexingOperationIT {

	@Test
	public void simple() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			expectContainedEntityLoadingIfRelevant( 1 );
			if ( !isDelete() ) {
				// Deletes don't trigger reindexing, so we don't expect anything for those.
				expectUpdateCausedByContained( futureFromBackend, 1, "1", "contained1" );
			}
			scenario().addWithoutInstanceTo( indexingPlan, ContainedEntity.class, 1 );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Test
	public void loadingDoesNotFindEntity() {
		assumeImplicitLoading();

		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			// Only add and update perform implicit loading, and if they cannot load the entity,
			// they are just skipped, assuming that a delete event will come later
			// and that containing entities will be updated to no longer reference the contained entity.
			expectContainedEntityLoadingIfRelevant( Collections.singletonList( 1 ), Collections.singletonList( null ) );
			scenario().addWithoutInstanceTo( indexingPlan, ContainedEntity.class, 1 );
		}
	}

	@Test
	public void nullProvidedId() {
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();
			assertThatThrownBy( () -> scenario().addWithoutInstanceTo( indexingPlan, ContainedEntity.class, null, null ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Invalid indexing request",
							"if the entity is null, the identifier must be provided explicitly" );
		}
	}

	// Provided routes are ignored for contained types
	@Test
	public void providedId_providedRoutes() {
		CompletableFuture<?> futureFromBackend = new CompletableFuture<>();
		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			expectContainedEntityLoadingIfRelevant( Collections.singletonList( 42 ),
					Collections.singletonList( ContainedEntity.of( 1 ) ) );
			if ( !isDelete() ) {
				// Deletes don't trigger reindexing, so we don't expect anything for those.
				expectUpdateCausedByContained( futureFromBackend, 1, "1", "contained1" );
			}
			scenario().addWithoutInstanceTo( indexingPlan, ContainedEntity.class, 42,
					DocumentRoutesDescriptor.of( DocumentRouteDescriptor.of( "UE-123" ),
							Arrays.asList( DocumentRouteDescriptor.of( "UE-121" ),
									DocumentRouteDescriptor.of( "UE-122" ),
									DocumentRouteDescriptor.of( "UE-123" ) ) ) );
			// The session will wait for completion of the indexing plan upon closing,
			// so we need to complete it now.
			futureFromBackend.complete( null );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3108")
	public void containingNotIndexed() {
		assumeImplicitRoutingEnabled();

		try ( SearchSession session = createSession() ) {
			SearchIndexingPlan indexingPlan = session.indexingPlan();

			expectContainedEntityLoadingIfRelevant( 1 );
			MyRoutingBridge.indexed = false;
			MyRoutingBridge.previouslyIndexed = false;
			// We don't expect the actual operation, which should be skipped because the containing entity is not indexed.
			scenario().addWithoutInstanceTo( indexingPlan, ContainedEntity.class, 1 );
		}
	}

	@Override
	protected boolean isImplicitRoutingEnabled() {
		return routingBinder != null && !isDelete();
	}

	private void assumeImplicitLoading() {
		assumeTrue( "This test only makes sense when "
				+ "the operation automatically loads entities",
				!isDelete() );
	}

}
