/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;

public class StubSearchAggregationBuilderFactory
		implements SearchAggregationBuilderFactory {
	@Override
	public <T> WithParametersAggregationBuilder<T> withParameters() {
		return new StubSearchAggregation.StubWithParametersAggregationBuilder<>();
	}
}
