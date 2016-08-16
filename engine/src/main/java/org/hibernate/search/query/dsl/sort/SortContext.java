/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortContext {

	/**
	 * Order elements by their relevance score in decreasing order by default.
	 */
	SortScoreContext byScore();

	/**
	 * Order elements by their internal index order.
	 * This is the fastest sorting.
	 */
	SortOrderTermination byIndexOrder();

	/**
	 * Order elements by value of a specific field. The sort field type will be determined automatically.
	 * @throws SearchException If the sort field type could not be guessed. This will happen in particular
	 * if a custom field bridge is defined on the given field.
	 * @see {@link #byField(String, org.apache.lucene.search.SortField.Type)} for fields with
	 * custom field bridges.
	 */
	SortFieldContext byField(String field);

	/**
	 * Order elements by value of a specific field, with the sort field type provided.
	 * <p><strong>Note:</strong> use of this method is only required when sorting on a
	 * field that uses a custom field bridge. Otherwise, one may simply use
	 * {@link #byField(String)}.
	 */
	SortFieldContext byField(String field, SortField.Type sortFieldType);

	/**
	 * Order elements by distance.
	 * <p>The distance is computed between the value of the given field (which must be
	 * a {@link Spatial} field) and reference coordinates, to be provided in the
	 * {@link SortDistanceFromContext next context}.
	 */
	SortDistanceFromContext byDistance(String field);

	/**
	 * Sort by distance from an already built spatial query.
	 * For distance sort without a query, use {@code byDistance("location").fromCoordinates(...)}
	 * or {@code byDistance("location").fromLatitude(...)}
	 */
	SortDistanceContext byDistanceFromSpatialQuery(Query query);

	/**
	 * Order element using the native backend API for Lucene.
	 */
	SortNativeContext byNative(SortField sortField);

	/**
	 * Order element using the native backend API for Elasticsearch.
	 */
	SortNativeContext byNative(String sortField);
}
