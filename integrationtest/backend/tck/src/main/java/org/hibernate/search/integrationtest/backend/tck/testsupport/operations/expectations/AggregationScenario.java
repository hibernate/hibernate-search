/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
