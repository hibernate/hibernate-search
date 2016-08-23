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
public interface SortAdditionalSortFieldContext {

	// navigation to next sort definition

	/**
	 * Order elements by a specific field
	 */
	SortFieldContext andByField(String field);

	/**
	 * Order elements by their relevance score in decreasing order by default.
	 */
	//TODO Is byScore terminal or can it be followed by other sorts?
	SortOrderTermination andByScore();

	/**
	 * Order elements by their internal index order.
	 * This is the fastest sorting.
	 */
	SortOrderTermination andByIndexOrder();

	/**
	 * Sort by distance from an already built spatial query.
	 * For distance sort without a query, use {@code byField("location").fromCoordinates(...)}
	 * or {@code byField("location").fromLatitude(...)}
	 */
	SortDistanceContext andByDistanceFromSpatialQuery(Query query);

	/**
	 * Order element using the native backend API for Lucene.
	 */
	SortNativeContext andByNative(SortField sortField);

	/**
	 * Order element using the native backend API for Elasticsearch.
	 */
	SortNativeContext andByNative(String sortField);
}
