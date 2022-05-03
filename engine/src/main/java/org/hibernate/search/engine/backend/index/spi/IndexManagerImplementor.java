/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.spi.SavedState;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface implemented by backends and provided to the engine.
 */
public interface IndexManagerImplementor {

	default SavedState saveForRestart() {
		// TODO HSEARCH-4545 Implement this method
		return SavedState.empty();
	}

	/**
	 * Start any resource necessary to operate the index manager at runtime.
	 * <p>
	 * Called by the engine once after bootstrap, after
	 * {@link org.hibernate.search.engine.backend.spi.BackendImplementor#start(BackendStartContext)}
	 * was called on the corresponding backend.
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

	IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext);

	IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext);

	void addTo(IndexScopeBuilder builder);

}
