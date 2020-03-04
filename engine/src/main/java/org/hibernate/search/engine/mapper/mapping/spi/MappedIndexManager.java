/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface provided to mappers to access the index manager.
 */
public interface MappedIndexManager {

	IndexManager toAPI();

	IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	IndexIndexer createIndexer(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy);

	IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext);

	<R, E> MappedIndexScopeBuilder<R, E> createScopeBuilder(BackendMappingContext mappingContext);

	void addTo(MappedIndexScopeBuilder<?, ?> builder);
}
