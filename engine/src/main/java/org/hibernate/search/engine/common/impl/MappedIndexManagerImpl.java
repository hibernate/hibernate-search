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
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
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
	public IndexWorkPlan<D> createWorkPlan(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return implementor.createWorkPlan( sessionContext, commitStrategy, refreshStrategy );
	}

	@Override
	public IndexDocumentWorkExecutor<D> createDocumentWorkExecutor(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy) {
		return implementor.createDocumentWorkExecutor( sessionContext, commitStrategy );
	}

	@Override
	public IndexWorkExecutor createWorkExecutor() {
		return implementor.createWorkExecutor();
	}

	@Override
	public <R, E> MappedIndexScopeBuilder<R, E> createScopeBuilder(MappingContextImplementor mappingContext) {
		return new MappedIndexScopeBuilderImpl<>(
				implementor, mappingContext
		);
	}

	@Override
	public void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		((MappedIndexScopeBuilderImpl<?, ?>) builder).add( implementor );
	}
}
