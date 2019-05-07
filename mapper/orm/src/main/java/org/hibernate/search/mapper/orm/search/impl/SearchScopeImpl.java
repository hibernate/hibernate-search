/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.dsl.query.impl.HibernateOrmSearchQueryResultDefinitionContextImpl;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class SearchScopeImpl<O> implements SearchScope<O> {

	private final PojoSearchScopeDelegate<O, O> delegate;
	private final SessionImplementor sessionImplementor;

	public SearchScopeImpl(PojoSearchScopeDelegate<O, O> delegate,
			SessionImplementor sessionImplementor) {
		this.delegate = delegate;
		this.sessionImplementor = sessionImplementor;
	}

	@Override
	public HibernateOrmSearchQueryResultDefinitionContext<O> search() {
		ObjectLoaderBuilder<O> objectLoaderBuilder =
				new ObjectLoaderBuilder<>( sessionImplementor, delegate.getIncludedIndexedTypes() );
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		HibernateOrmLoadingContext.Builder<O> loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				sessionImplementor, delegate, objectLoaderBuilder, loadingOptions
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
	public SearchSortContainerContext sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactoryContext<PojoReference, O> projection() {
		return delegate.projection();
	}
}
