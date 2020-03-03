/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.schema.management.impl.SearchSchemaManagerImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQuerySelectStep;
import org.hibernate.search.mapper.orm.search.query.dsl.impl.HibernateOrmSearchQuerySelectStepImpl;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.orm.work.impl.SearchWorkspaceImpl;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class SearchScopeImpl<E> implements SearchScope<E> {

	private final HibernateOrmScopeMappingContext mappingContext;
	private final PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(HibernateOrmScopeMappingContext mappingContext,
			PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.delegate = delegate;
	}

	public HibernateOrmSearchQuerySelectStep<E> search(HibernateOrmScopeSessionContext sessionContext) {
		HibernateOrmLoadingContext.Builder<E> loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				mappingContext, sessionContext, delegate.getIncludedIndexedTypes()
		);
		return new HibernateOrmSearchQuerySelectStepImpl<>(
				delegate.search( sessionContext.getBackendSessionContext(), loadingContextBuilder ),
				loadingContextBuilder
		);
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactory<EntityReference, E> projection() {
		return delegate.projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	@Override
	public SearchSchemaManager schemaManager() {
		return new SearchSchemaManagerImpl( schemaManagerDelegate() );
	}

	@Override
	public SearchWorkspace workspace() {
		return workspace( (String) null );
	}

	@Override
	public SearchWorkspace workspace(String tenantId) {
		return workspace( mappingContext.getDetachedBackendSessionContext( tenantId ) );
	}

	public SearchWorkspace workspace(DetachedBackendSessionContext detachedSessionContext) {
		return new SearchWorkspaceImpl( delegate.workspace( detachedSessionContext ) );
	}

	@Override
	public MassIndexer massIndexer() {
		return massIndexer( (String) null );
	}

	@Override
	public MassIndexer massIndexer(String tenantId) {
		return massIndexer( mappingContext.getDetachedBackendSessionContext( tenantId ) );
	}

	public MassIndexer massIndexer(DetachedBackendSessionContext detachedSessionContext) {
		return new MassIndexerImpl(
				mappingContext,
				delegate.getIncludedIndexedTypes(),
				detachedSessionContext,
				delegate.workspace( detachedSessionContext )
		);
	}

	public PojoScopeSchemaManager schemaManagerDelegate() {
		return delegate.schemaManager();
	}
}
