/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.spatial.GeoPoint;

public interface DistanceSortBuilder extends SearchSortBuilder {

	void center(GeoPoint center);

	void order(SortOrder order);

	void missingFirst();

	void missingLast();

	void missingHighest();

	void missingLowest();

	void missingAs(GeoPoint value);

	void mode(SortMode mode);

	void filter(SearchPredicate filter);

}
