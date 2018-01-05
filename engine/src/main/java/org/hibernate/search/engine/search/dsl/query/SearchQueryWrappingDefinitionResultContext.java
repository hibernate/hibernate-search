/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query;

import java.util.function.Function;

/**
 * The context used when building a query, when a query wrapper can be defined.
 * <p>
 * The query wrapper will be applied when the query is finally built.
 */
public interface SearchQueryWrappingDefinitionResultContext<Q> extends SearchQueryResultContext<Q> {

	<R> SearchQueryResultContext<R> asWrappedQuery(Function<Q, R> wrapperFactory);

}
