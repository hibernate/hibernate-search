/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.FullTextQueryResultDefinitionContext;
import org.hibernate.search.mapper.orm.impl.FullTextQueryImpl;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;

class FullTextQueryResultDefinitionContextImpl<O>
		implements FullTextQueryResultDefinitionContext<O> {
	private final PojoSearchTargetDelegate<O, O> searchTargetDelegate;
	private final SessionImplementor sessionImplementor;
	private final ObjectLoaderBuilder<O> objectLoaderBuilder;

	FullTextQueryResultDefinitionContextImpl(
			PojoSearchTargetDelegate<O, O> searchTargetDelegate,
			SessionImplementor sessionImplementor) {
		this.searchTargetDelegate = searchTargetDelegate;
		this.sessionImplementor = sessionImplementor;
		this.objectLoaderBuilder = new ObjectLoaderBuilder<>( sessionImplementor, searchTargetDelegate.getTargetedIndexedTypes() );
	}

	@Override
	public SearchQueryResultContext<? extends FullTextQuery<O>> asEntity() {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchTargetDelegate.queryAsLoadedObject(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions )
		);
	}

	@Override
	public <T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjection(SearchProjection<T> projection) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchTargetDelegate.queryAsProjection(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ),
				projection
		);
	}

	@Override
	public <T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, O>, SearchProjection<T>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchTargetDelegate.projection() ) );
	}

	@Override
	public SearchQueryResultContext<? extends FullTextQuery<List<?>>> asProjections(
			SearchProjection<?>... projections) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return searchTargetDelegate.queryAsProjections(
				objectLoaderBuilder.build( loadingOptions ),
				q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ),
				projections
		);
	}
}
