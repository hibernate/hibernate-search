/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.SortField;
import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortContext {

	/**
	 * Order elements by their relevance score.
	 *
	 * <p>The default order is <strong>descending</strong>, i.e. higher scores come first.
	 */
	SortScoreContext byScore();

	/**
	 * Order elements by their internal index order.
	 */
	SortOrderTermination byIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 *
	 * @param fieldName The name of the index field to sort by
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
	 */
	SortDistanceNoFieldContext byDistance();

	/**
	 * Order element using the native backend API for Lucene.
	 *
	 * <p>The sort order (ascending/descending) is defined in <code>sortField</code>
	 *
	 * @param sortField The sort field to be added to the sort list.
	 */
	SortNativeContext byNative(SortField sortField);

	/**
	 * Order element using the native backend API for Elasticsearch.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @param sortFieldNativeDescription The sort field description, as valid JSON. This may be a
	 * simple quoted string (e.g. "'asc'") or a JSON map (e.g. "{'key': 'value'}"). See
	 * Elasticsearch's documentation for information about the exact syntax.
	 */
	SortNativeContext byNative(String fieldName, String sortFieldNativeDescription);
}
