/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface implemented by backends and provided to the engine.
 */
public interface IndexManagerImplementor<D extends DocumentElement> extends AutoCloseable {

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
	 * @return The object that should be exposed as API to users.
	 */
	IndexManager toAPI();

	IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	IndexDocumentWorkExecutor<D> createDocumentWorkExecutor(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy);

	IndexWorkExecutor createWorkExecutor();

	IndexScopeBuilder createScopeBuilder(MappingContextImplementor mappingContext);

	void addTo(IndexScopeBuilder builder);

	@Override
	void close();

}
