/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortContext {

	/**
	 * Order elements by their relevance score in decreasing order by default.
	 */
	SortOrderTermination byScore();

	/**
	 * Order elements by their internal index order.
	 * This is the fastest sorting.
	 */
	SortOrderTermination byIndexOrder();

	/**
	 * Order elements by a specific field
	 */
	SortFieldContext byField(String field);

	/**
	 * Sort by distance from an already built spatial query.
	 * For distance sort without a query, use {@code byField("location").fromCoordinates(...)}
	 * or {@code byField("location").fromLatitude(...)}
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
