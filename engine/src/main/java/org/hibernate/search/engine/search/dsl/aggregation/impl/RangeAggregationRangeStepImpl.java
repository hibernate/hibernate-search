/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.aggregation.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.dsl.aggregation.RangeAggregationRangeMoreStep;
import org.hibernate.search.engine.search.dsl.aggregation.RangeAggregationRangeStep;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.impl.Contracts;

class RangeAggregationRangeStepImpl<F> implements RangeAggregationRangeStep<F>, RangeAggregationRangeMoreStep<F> {
	private final RangeAggregationBuilder<F> builder;

	RangeAggregationRangeStepImpl(RangeAggregationBuilder<F> builder) {
		this.builder = builder;
	}

	@Override
	public RangeAggregationRangeMoreStep<F> range(Range<? extends F> range) {
		Contracts.assertNotNull( range, "range" );
		builder.range( range );
		return this;
	}

	@Override
	public RangeAggregationRangeMoreStep<F> ranges(Collection<? extends Range<? extends F>> ranges) {
		Contracts.assertNotNull( ranges, "ranges" );
		for ( Range<? extends F> range : ranges ) {
			range( range );
		}
		return this;
	}

	@Override
	public SearchAggregation<Map<Range<F>, Long>> toAggregation() {
		return builder.build();
	}
}
