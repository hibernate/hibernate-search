/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public interface AggregationScenario<A> {

	default AggregationFinalStep<A> setup(TypedSearchAggregationFactory<?> factory, String fieldPath) {
		return setup( factory, fieldPath, null );
	}

	AggregationFinalStep<A> setup(TypedSearchAggregationFactory<?> factory, String fieldPath,
			Function<? super TypedSearchPredicateFactory<?>, ? extends PredicateFinalStep> filterOrNull);

	AggregationFinalStep<A> setupWithConverterSetting(TypedSearchAggregationFactory<?> factory, String fieldPath,
			ValueModel valueModel);

	void check(A aggregationResult);

}
