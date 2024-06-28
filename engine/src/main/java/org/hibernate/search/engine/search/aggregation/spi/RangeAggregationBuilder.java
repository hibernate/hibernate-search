/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.Map;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.data.Range;

public interface RangeAggregationBuilder<K> extends SearchAggregationBuilder<Map<Range<K>, Long>> {

	interface TypeSelector {
		<K> RangeAggregationBuilder<K> type(Class<K> expectedType, ValueModel valueModel);
	}

	void filter(SearchPredicate filter);

	void range(Range<? extends K> range);
}
