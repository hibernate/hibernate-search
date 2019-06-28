/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.dsl.query.impl.HibernateOrmSearchQueryHitTypeStepImpl;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.orm.writing.impl.SearchWriterImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class SearchScopeImpl<E> implements SearchScope<E>, org.hibernate.search.mapper.orm.search.SearchScope<E> {

	private final HibernateOrmScopeMappingContext mappingContext;
	private final HibernateOrmScopeSessionContext sessionContext;
	private final PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(HibernateOrmScopeMappingContext mappingContext,
			HibernateOrmScopeSessionContext sessionContext,
			PojoScopeDelegate<EntityReference, E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> search() {
		HibernateOrmLoadingContext.Builder<E> loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				sessionContext, delegate.getIncludedIndexedTypes()
		);
		return new HibernateOrmSearchQueryHitTypeStepImpl<>(
				delegate.search( loadingContextBuilder ),
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
	public SearchWriter writer() {
		return new SearchWriterImpl( delegate.executor() );
	}

	@Override
	public MassIndexer massIndexer() {
		return new MassIndexerImpl(
				sessionContext.getSession().getFactory(),
				mappingContext,
				delegate.getIncludedIndexedTypes(),
				sessionContext.getDetachedSessionContext(),
				delegate.executor()
		);
	}
}
