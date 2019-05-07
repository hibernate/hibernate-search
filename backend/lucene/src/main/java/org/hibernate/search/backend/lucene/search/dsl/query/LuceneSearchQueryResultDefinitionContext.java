/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public interface LuceneSearchQueryResultDefinitionContext<R, O>
		extends SearchQueryResultDefinitionContext<R, O, LuceneSearchProjectionFactoryContext<R, O>> {

	@Override
	LuceneSearchQueryResultContext<O> asEntity();

	@Override
	LuceneSearchQueryResultContext<R> asReference();

	@Override
	<P> LuceneSearchQueryResultContext<P> asProjection(
			Function<? super LuceneSearchProjectionFactoryContext<R, O>, ? extends SearchProjectionTerminalContext<P>> projectionContributor);

	@Override
	<P> LuceneSearchQueryResultContext<P> asProjection(SearchProjection<P> projection);

	@Override
	LuceneSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections);

}
