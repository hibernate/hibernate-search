/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.mapper.javabean.search.dsl.query.JavaBeanQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;

public class JavaBeanQueryResultDefinitionContextImpl implements JavaBeanQueryResultDefinitionContext {
	private final PojoSearchTargetDelegate<?, PojoReference> delegate;

	public JavaBeanQueryResultDefinitionContextImpl(PojoSearchTargetDelegate<?, PojoReference> delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQueryResultContext<SearchQuery<PojoReference>> asReferences() {
		return asReferences( Function.identity() );
	}

	@Override
	public <T> SearchQueryResultContext<SearchQuery<T>> asReferences(Function<PojoReference, T> hitTransformer) {
		return delegate.queryAsReferences( hitTransformer, Function.identity() );
	}

	@Override
	public <P> SearchQueryResultContext<SearchQuery<P>> asProjections(SearchProjection<P> projection) {
		return asProjections( Function.identity(), projection );
	}

	@Override
	public <P, T> SearchQueryResultContext<SearchQuery<T>> asProjections(Function<P, T> hitTransformer,
			SearchProjection<P> projection) {
		return delegate.queryAsProjections(
				ObjectLoader.identity(), hitTransformer, Function.identity(), projection
		);
	}

	@Override
	public SearchQueryResultContext<SearchQuery<List<?>>> asProjections(SearchProjection<?>... projections) {
		return asProjections( Function.identity(), projections );
	}

	@Override
	public <T> SearchQueryResultContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer,
			SearchProjection<?>... projections) {
		return delegate.queryAsProjections(
				ObjectLoader.identity(), hitTransformer, Function.identity(), projections
		);
	}
}
