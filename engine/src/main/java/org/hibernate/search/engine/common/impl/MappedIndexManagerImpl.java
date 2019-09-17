/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

class MappedIndexManagerImpl<D extends DocumentElement> implements MappedIndexManager<D> {

	private final IndexManagerImplementor<D> implementor;

	MappedIndexManagerImpl(IndexManagerImplementor<D> implementor) {
		this.implementor = implementor;
	}

	@Override
	public IndexManager toAPI() {
		return implementor.toAPI();
	}

	@Override
	public IndexIndexingPlan<D> createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return implementor.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
	}

	@Override
	public IndexIndexer<D> createIndexer(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy) {
		return implementor.createIndexer( sessionContext, commitStrategy );
	}

	@Override
	public IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext) {
		return implementor.createWorkspace( sessionContext );
	}

	@Override
	public <R, E> MappedIndexScopeBuilder<R, E> createScopeBuilder(BackendMappingContext mappingContext) {
		return new MappedIndexScopeBuilderImpl<>(
				implementor, mappingContext
		);
	}

	@Override
	public void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		((MappedIndexScopeBuilderImpl<?, ?>) builder).add( implementor );
	}
}
