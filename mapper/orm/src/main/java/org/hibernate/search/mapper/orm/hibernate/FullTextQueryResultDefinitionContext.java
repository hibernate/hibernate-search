/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.hibernate;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;

/**
 * @author Yoann Rodiere
 */
public interface FullTextQueryResultDefinitionContext<O>
		extends org.hibernate.search.mapper.orm.jpa.FullTextQueryResultDefinitionContext<O> {

	@Override
	default SearchQueryResultContext<? extends FullTextQuery<O>> asEntity() {
		return asEntity( Function.identity() );
	}

	@Override
	default <P> SearchQueryResultContext<? extends FullTextQuery<P>> asProjection(SearchProjection<P> projection) {
		return asProjection( Function.identity(), projection );
	}

	@Override
	default SearchQueryResultContext<? extends FullTextQuery<List<?>>> asProjections(SearchProjection<?>... projections) {
		return asProjections( Function.identity(), projections );
	}

	@Override
	<T> SearchQueryResultContext<? extends FullTextQuery<T>> asEntity(Function<O, T> hitTransformer);

	@Override
	<P, T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjection(
			Function<P, T> hitTransformer,
			SearchProjection<P> projection);

	@Override
	<T> SearchQueryResultContext<? extends FullTextQuery<T>> asProjections(
			Function<List<?>, T> hitTransformer,
			SearchProjection<?>... projections);

}
