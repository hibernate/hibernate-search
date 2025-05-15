/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.Map;

import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

public class TermsAggregationFieldStepImpl<SR, PDF extends TypedSearchPredicateFactory<SR>>
		implements TermsAggregationFieldStep<SR, PDF> {
	private final SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext;

	public TermsAggregationFieldStepImpl(SearchAggregationDslContext<SR, ?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <F> TermsAggregationOptionsStep<SR, ?, PDF, F, Map<F, Long>> field(String fieldPath, Class<F> type,
			ValueModel valueModel) {
		Contracts.assertNotNull( fieldPath, "fieldPath" );
		Contracts.assertNotNull( type, "type" );
		TermsAggregationBuilder<F> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.TERMS ).type( type, valueModel );
		return new TermsAggregationOptionsStepImpl<>( builder, dslContext );
	}
}
