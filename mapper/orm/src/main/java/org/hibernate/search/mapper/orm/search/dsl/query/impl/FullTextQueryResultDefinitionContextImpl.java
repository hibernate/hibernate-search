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
import org.hibernate.search.mapper.orm.search.query.FullTextQuery;
import org.hibernate.search.mapper.orm.search.dsl.query.FullTextQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.search.query.impl.FullTextQueryImpl;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class FullTextQueryResultDefinitionContextImpl<O>
		implements FullTextQueryResultDefinitionContext<O> {
	private final PojoSearchScopeDelegate<O, O> searchScopeDelegate;
	private final SessionImplementor sessionImplementor;
	private final ObjectLoaderBuilder<O> objectLoaderBuilder;

	public FullTextQueryResultDefinitionContextImpl(
			PojoSearchScopeDelegate<O, O> searchScopeDelegate,
			SessionImplementor sessionImplementor) {
		this.searchScopeDelegate = searchScopeDelegate;
		this.sessionImplementor = sessionImplementor;
		this.objectLoaderBuilder = new ObjectLoaderBuilder<>( sessionImplementor, searchScopeDelegate.getIncludedIndexedTypes() );
	}

	@Override
	public SearchQueryResultContext<? extends FullTextQuery<O>> asEntity() {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchScopeDelegate.queryAsLoadedObject(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions )
		);
	}

	@Override
	public <T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjection(SearchProjection<T> projection) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchScopeDelegate.queryAsProjection(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ),
				projection
		);
	}

	@Override
	public <T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, O>, ? extends SearchProjectionTerminalContext<T>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	@Override
	public SearchQueryResultContext<? extends FullTextQuery<List<?>>> asProjections(
			SearchProjection<?>... projections) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchScopeDelegate.queryAsProjections(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ),
				projections
		);
	}
}
