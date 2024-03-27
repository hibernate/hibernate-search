/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public interface AggregationScenario<A> {

	default AggregationFinalStep<A> setup(SearchAggregationFactory factory, String fieldPath) {
		return setup( factory, fieldPath, null );
	}

	AggregationFinalStep<A> setup(SearchAggregationFactory factory, String fieldPath,
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> filterOrNull);

	AggregationFinalStep<A> setupWithConverterSetting(SearchAggregationFactory factory, String fieldPath,
			ValueConvert convert);

	void check(A aggregationResult);

}
