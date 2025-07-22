/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.CountDocumentsAggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.CountDocumentAggregationBuilder;

public class CountDocumentsAggregationFinalStepImpl
		implements CountDocumentsAggregationFinalStep {
	private final CountDocumentAggregationBuilder builder;

	public CountDocumentsAggregationFinalStepImpl(SearchAggregationDslContext<?, ?, ?> dslContext) {
		this.builder = dslContext.scope().rootQueryElement( AggregationTypeKeys.COUNT_DOCUMENTS ).builder();
	}

	@Override
	public SearchAggregation<Long> toAggregation() {
		return builder.build();
	}
}
