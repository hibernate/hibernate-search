/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * @author Hardy Ferentschik
 *
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your facets (now called aggregations)
 * using {@link SearchQueryOptionsStep#aggregation(AggregationKey, Function)}.
 * You can then fetch the query result using {@link SearchQuery#fetch(Integer)}
 * and get each aggregation using {@link SearchResult#aggregation(AggregationKey)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface FacetContext {
	/**
	 * @param name the name for this facet request
	 *
	 * @return a {@code FacetFieldContext} to continue building the facet request
	 */
	FacetFieldContext name(String name);
}

