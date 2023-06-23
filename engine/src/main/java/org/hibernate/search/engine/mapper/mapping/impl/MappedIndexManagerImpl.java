/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.util.Set;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.mapper.scope.impl.MappedIndexScopeBuilderImpl;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;

public class MappedIndexManagerImpl implements MappedIndexManager {

	private final IndexManagerImplementor implementor;

	public MappedIndexManagerImpl(IndexManagerImplementor implementor) {
		this.implementor = implementor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "implementor=" + implementor
				+ "]";
	}

	@Override
	public IndexManager toAPI() {
		return implementor.toAPI();
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return implementor.schemaManager();
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return implementor.createIndexingPlan( sessionContext, commitStrategy, refreshStrategy );
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext sessionContext) {
		return implementor.createIndexer( sessionContext );
	}

	@Override
	@Deprecated
	public IndexWorkspace createWorkspace(
			org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext sessionContext) {
		return createWorkspace( sessionContext.mappingContext(), asSetIgnoreNull( sessionContext.tenantIdentifier() ) );
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		return implementor.createWorkspace( mappingContext, tenantIds );
	}

	@Override
	public <R, E> MappedIndexScopeBuilder<R, E> createScopeBuilder(BackendMappingContext mappingContext) {
		return new MappedIndexScopeBuilderImpl<>(
				implementor, mappingContext
		);
	}

	@Override
	public void addTo(MappedIndexScopeBuilder<?, ?> builder) {
		( (MappedIndexScopeBuilderImpl<?, ?>) builder ).add( implementor );
	}
}
