/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.common.NamedValues;

public interface WithParametersAggregationBuilder<T> extends SearchAggregationBuilder<T> {
	void creator(Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator);
}
