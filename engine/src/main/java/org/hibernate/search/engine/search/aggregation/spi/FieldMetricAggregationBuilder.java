/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.common.ValueModel;

/**
 * Used by the all the metric aggregations that are supposed to return a value having the same type of field,
 * or if a projection converter is present and enabled, the same type of the latter.
 *
 * @param <A> the type of the result of the aggregation
 */
public interface FieldMetricAggregationBuilder<A> extends SearchFilterableAggregationBuilder<A> {

	interface TypeSelector {
		<A> FieldMetricAggregationBuilder<A> type(Class<A> expectedType, ValueModel valueModel);
	}

}
