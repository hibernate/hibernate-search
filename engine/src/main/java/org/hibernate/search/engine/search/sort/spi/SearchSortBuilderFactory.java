/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A factory for search sort builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search sorts.
 *
 * @param <C> The type of query element collector
 */
public interface SearchSortBuilderFactory<C> {

	/**
	 * Contribute a sort builder to a collector.
	 * <p>
	 * May be called multiple times per collector, if there are multiple sorts.
	 *  @param collector The query element collector.
	 * @param sort The sort builder implementation.
	 */
	void contribute(C collector, SearchSort sort);

	ScoreSortBuilder score();

	FieldSortBuilder field(String absoluteFieldPath);

	DistanceSortBuilder distance(String absoluteFieldPath, GeoPoint location);

	SearchSort indexOrder();

	CompositeSortBuilder composite();

}
