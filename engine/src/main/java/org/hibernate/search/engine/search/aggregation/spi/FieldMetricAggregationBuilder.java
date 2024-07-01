/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import org.hibernate.search.engine.search.common.ValueConvert;

/**
 * Used by the all the aggregation metrics that are supposed to return a value having the same type of field,
 * or if a projection converter is present and enabled, the same type of the latter.
 *
 * @param <A> the type of the result of the aggregation
 */
public interface FieldMetricAggregationBuilder<A> extends SearchFilterableAggregationBuilder<A> {

	interface TypeSelector {
		<A> FieldMetricAggregationBuilder<A> type(Class<A> expectedType, ValueConvert convert);
	}

}
