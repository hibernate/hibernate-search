/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.util.common.impl.Contracts;

class RangeAggregationFieldStepImpl implements RangeAggregationFieldStep {
	private final SearchAggregationDslContext<?> dslContext;

	RangeAggregationFieldStepImpl(SearchAggregationDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <F> RangeAggregationRangeStep<F> field(String absoluteFieldPath, Class<F> type, ValueConvert convert) {
		Contracts.assertNotNull( absoluteFieldPath, "absoluteFieldPath" );
		Contracts.assertNotNull( type, "type" );
		RangeAggregationBuilder<F> builder =
				dslContext.getBuilderFactory().createRangeAggregationBuilder( absoluteFieldPath, type, convert );
		return new RangeAggregationRangeStepImpl<>( builder );
	}
}
