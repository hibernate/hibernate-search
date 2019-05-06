/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.query.impl.HibernateOrmSearchQuery;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class SearchQueryResultDefinitionContextImpl<O>
		implements SearchQueryResultDefinitionContext<O> {
	private final PojoSearchScopeDelegate<O, O> searchScopeDelegate;
	private final SessionImplementor sessionImplementor;
	private final HibernateOrmLoadingContext.Builder<O> loadingContextBuilder;
	private final MutableObjectLoadingOptions loadingOptions;

	public SearchQueryResultDefinitionContextImpl(
			PojoSearchScopeDelegate<O, O> searchScopeDelegate,
			SessionImplementor sessionImplementor) {
		this.searchScopeDelegate = searchScopeDelegate;
		this.sessionImplementor = sessionImplementor;
		ObjectLoaderBuilder<O> objectLoaderBuilder =
				new ObjectLoaderBuilder<>( sessionImplementor, searchScopeDelegate.getIncludedIndexedTypes() );
		this.loadingOptions = new MutableObjectLoadingOptions();
		this.loadingContextBuilder = new HibernateOrmLoadingContext.Builder<>(
				sessionImplementor, searchScopeDelegate, objectLoaderBuilder, loadingOptions
		);
	}

	@Override
	public SearchQueryResultContext<?, ? extends SearchQuery<O>, ?> asEntity() {
		return searchScopeDelegate.queryAsLoadedObject(
				loadingContextBuilder,
				q -> new HibernateOrmSearchQuery<>( q, sessionImplementor, loadingOptions )
		);
	}

	@Override
	public <T> SearchQueryResultContext<?, ? extends SearchQuery<T>, ?> asProjection(SearchProjection<T> projection) {
		return searchScopeDelegate.queryAsProjection(
				loadingContextBuilder,
				q -> new HibernateOrmSearchQuery<>( q, sessionImplementor, loadingOptions ),
				projection
		);
	}

	@Override
	public <T> SearchQueryResultContext<?, ? extends SearchQuery<T>, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, O>, ? extends SearchProjectionTerminalContext<T>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	@Override
	public SearchQueryResultContext<?, ? extends SearchQuery<List<?>>, ?> asProjections(
			SearchProjection<?>... projections) {
		return searchScopeDelegate.queryAsProjections(
				loadingContextBuilder,
				q -> new HibernateOrmSearchQuery<>( q, sessionImplementor, loadingOptions ),
				projections
		);
	}

	@Override
	public SearchQueryResultDefinitionContext<O> fetchSize(int fetchSize) {
		loadingOptions.setFetchSize( fetchSize );
		return this;
	}
}
