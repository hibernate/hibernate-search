/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import javax.persistence.EntityManager;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.query.dsl.impl.HibernateOrmSearchQueryHitTypeStepImpl;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.orm.writing.impl.SearchWriterImpl;
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

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> search(EntityManager entityManager) {
		HibernateOrmScopeSessionContext sessionContext = mappingContext.getSessionContext( entityManager );
		return search( sessionContext );
	}

	public HibernateOrmSearchQueryHitTypeStep<E> search(HibernateOrmScopeSessionContext sessionContext) {
		HibernateOrmLoadingContext.Builder<E> loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				mappingContext, sessionContext, delegate.getIncludedIndexedTypes()
		);
		return new HibernateOrmSearchQueryHitTypeStepImpl<>(
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
	public SearchWriter writer(EntityManager entityManager) {
		HibernateOrmScopeSessionContext sessionContext = mappingContext.getSessionContext( entityManager );
		return writer( sessionContext );
	}

	public SearchWriter writer(HibernateOrmScopeSessionContext sessionContext) {
		return new SearchWriterImpl( delegate.executor( sessionContext.getDetachedBackendSessionContext() ) );
	}

	@Override
	public MassIndexer massIndexer(EntityManager entityManager) {
		HibernateOrmScopeSessionContext sessionContext = mappingContext.getSessionContext( entityManager );
		return massIndexer( sessionContext );
	}

	public MassIndexer massIndexer(HibernateOrmScopeSessionContext sessionContext) {
		DetachedBackendSessionContext detachedSessionContext = sessionContext.getDetachedBackendSessionContext();
		return new MassIndexerImpl(
				mappingContext,
				delegate.getIncludedIndexedTypes(),
				detachedSessionContext,
				delegate.executor( detachedSessionContext )
		);
	}
}
