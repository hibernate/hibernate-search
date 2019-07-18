/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.dsl.aggregation.AggregationFinalStep;
import org.hibernate.search.engine.search.dsl.aggregation.SearchAggregationFactory;

public interface AggregationScenario<A> {

	AggregationFinalStep<A> setup(SearchAggregationFactory factory, String fieldPath);

	AggregationFinalStep<A> setupWithConverterSetting(SearchAggregationFactory factory, String fieldPath,
			ValueConvert convert);

	void check(A aggregationResult);

}
