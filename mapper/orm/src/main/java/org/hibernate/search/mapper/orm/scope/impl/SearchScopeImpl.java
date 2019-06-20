/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexerImpl;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.dsl.query.impl.HibernateOrmSearchQueryResultDefinitionContextImpl;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderBuilder;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.orm.writing.impl.SearchWriterImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public class SearchScopeImpl<E> implements SearchScope<E>, org.hibernate.search.mapper.orm.search.SearchScope<E> {

	private final HibernateOrmScopeMappingContext mappingContext;
	private final HibernateOrmScopeSessionContext sessionContext;
	private final PojoScopeDelegate<E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(HibernateOrmScopeMappingContext mappingContext,
			HibernateOrmScopeSessionContext sessionContext,
			PojoScopeDelegate<E, HibernateOrmScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public HibernateOrmSearchQueryResultDefinitionContext<E> search() {
		SessionImplementor sessionImplementor = sessionContext.getSession();
		EntityLoaderBuilder<E> entityLoaderBuilder =
				new EntityLoaderBuilder<>( sessionImplementor, delegate.getIncludedIndexedTypes() );
		MutableEntityLoadingOptions loadingOptions = new MutableEntityLoadingOptions();
		HibernateOrmLoadingContext.Builder<E> loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				sessionImplementor, delegate, entityLoaderBuilder, loadingOptions
		);
		return new HibernateOrmSearchQueryResultDefinitionContextImpl<>(
				delegate.search( loadingContextBuilder ),
				loadingOptions
		);
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactoryContext sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, E> projection() {
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
