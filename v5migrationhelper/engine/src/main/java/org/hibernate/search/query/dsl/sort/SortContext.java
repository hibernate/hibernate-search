/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import java.util.function.Function;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.search.SortField;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your sorts using {@link SearchQueryOptionsStep#sort(Function)}.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public interface SortContext {

	/**
	 * Order elements by their relevance score.
	 *
	 * <p>The default order is <strong>descending</strong>, i.e. higher scores come first.
	 * @return a context instance for building the sort
	 */
	SortScoreContext byScore();

	/**
	 * Order elements by their internal index order.
	 * @return a context instance for building the sort
	 */
	SortOrderTermination byIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @return a context instance for building the sort
	 * @throws SearchException If the sort field type could not be automatically determined.
	 */
	SortFieldContext byField(String fieldName);

	/**
	 * Order elements by distance.
	 *
	 * <p>The default order is <strong>ascending</strong>, i.e. shorter distances
	 * come first.
	 * <p>The distance is computed between the value of a field carrying coordinates
	 * (to be provided in the {@link SortDistanceNoFieldContext next context})
	 * and reference coordinates.
	 * @return a context instance for building the sort
	 */
	SortDistanceNoFieldContext byDistance();

	/**
	 * Order element using the native backend API for Lucene.
	 *
	 * <p>The sort order (ascending/descending) is defined in <code>sortField</code>
	 *
	 * @param sortField The sort field to be added to the sort list.
	 * @return a context instance for building the sort
	 */
	SortNativeContext byNative(SortField sortField);
}
