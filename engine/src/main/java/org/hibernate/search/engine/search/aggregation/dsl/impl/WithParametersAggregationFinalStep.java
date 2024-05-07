/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;
import org.hibernate.search.engine.search.common.NamedValues;

public class WithParametersAggregationFinalStep<E, A> implements AggregationFinalStep<A> {

	private final WithParametersAggregationBuilder<A> builder;

	public WithParametersAggregationFinalStep(
			SearchAggregationDslContext<E, ?, ?> dslContext,
			Function<? super NamedValues, ? extends AggregationFinalStep<A>> aggregationCreator) {
		builder = dslContext.scope().aggregationBuilders().withParameters();
		builder.creator( aggregationCreator );
	}

	@Override
	public SearchAggregation<A> toAggregation() {
		return builder.build();
	}
}
