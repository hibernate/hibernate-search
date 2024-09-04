/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface SearchFilterableAggregationBuilder<A> extends SearchAggregationBuilder<A> {

	void filter(SearchPredicate filter);

}
