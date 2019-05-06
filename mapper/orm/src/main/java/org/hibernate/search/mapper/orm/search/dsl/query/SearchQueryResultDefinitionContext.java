/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.orm.search.query.SearchQuery;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface SearchQueryResultDefinitionContext<O> {

	SearchQueryResultContext<?, ? extends SearchQuery<O>, ?> asEntity();

	<T> SearchQueryResultContext<?, ? extends SearchQuery<T>, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, O>, ? extends SearchProjectionTerminalContext<T>> projectionContributor);

	<T> SearchQueryResultContext<?, ? extends SearchQuery<T>, ?> asProjection(SearchProjection<T> projection);

	SearchQueryResultContext<?, ? extends SearchQuery<List<?>>, ?> asProjections(SearchProjection<?>... projections);

}
