/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.impl.Contracts;

public class RangeAggregationFieldStepImpl<PDF extends SearchPredicateFactory> implements RangeAggregationFieldStep<PDF> {
	private final SearchAggregationDslContext<?, ? extends PDF> dslContext;

	public RangeAggregationFieldStepImpl(SearchAggregationDslContext<?, ? extends PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public <F> RangeAggregationRangeStep<?, PDF, F> field(String fieldPath, Class<F> type, ValueConvert convert) {
		Contracts.assertNotNull( fieldPath, "fieldPath" );
		Contracts.assertNotNull( type, "type" );
		RangeAggregationBuilder<F> builder = dslContext.scope()
				.fieldQueryElement( fieldPath, AggregationTypeKeys.RANGE ).type( type, convert );
		return new RangeAggregationRangeStepImpl<>( builder, dslContext );
	}
}
