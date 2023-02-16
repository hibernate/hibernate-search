/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.common.resources.spi.SavedState;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface implemented by backends and provided to the engine.
 */
public interface IndexManagerImplementor {

	default SavedState saveForRestart() {
		return SavedState.empty();
	}

	/**
	 * Starts a subset of resources that are necessary to operate the index manager at runtime, and are expected to be reused upon restarts.
	 * The resources may be retrieved them from the saved state,
	 * or created if they are not present in the saved state.
	 * <p>
	 * Called by the engine once after bootstrap, after
	 * {@link org.hibernate.search.engine.backend.spi.BackendImplementor#start(BackendStartContext)}
	 * was called on the corresponding backend.
	 *
	 * @param context The start context.
	 * @param savedState The saved state returned by the corresponding index manager in the Hibernate Search integration
	 * being restarted, or {@link SavedState#empty()} on the first start.
	 */
	default void preStart(IndexManagerStartContext context, SavedState savedState) {
		// do nothing by default
	}

	/**
	 * Start any resource necessary to operate the index manager at runtime.
	 * <p>
	 * Called by the engine once just after
	 * {@link #preStart(IndexManagerStartContext, SavedState)}.
	 *
	 * @param context The start context.
	 */
	void start(IndexManagerStartContext context);

	/**
	 * Prepare for {@link #stop()}.
	 *
	 * @return A future that completes when ongoing works complete.
	 */
	CompletableFuture<?> preStop();

	/**
	 * Stop and release any resource necessary to operate the backend at runtime.
	 * <p>
	 * Called by the engine once before shutdown.
	 */
	void stop();

	/**
	 * @return The object that should be exposed as API to users.
	 */
	IndexManager toAPI();

	IndexSchemaManager schemaManager();

	IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	IndexIndexer createIndexer(BackendSessionContext sessionContext);

	IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantId);

	IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext);

	void addTo(IndexScopeBuilder builder);

}
