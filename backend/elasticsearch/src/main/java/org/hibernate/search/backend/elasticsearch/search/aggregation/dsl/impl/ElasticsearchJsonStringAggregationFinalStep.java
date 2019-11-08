/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilder;

final class ElasticsearchJsonStringAggregationFinalStep
		implements AggregationFinalStep<String> {
	private final SearchAggregationBuilder<String> builder;

	ElasticsearchJsonStringAggregationFinalStep(SearchAggregationBuilder<String> builder) {
		this.builder = builder;
	}

	@Override
	public SearchAggregation<String> toAggregation() {
		return builder.build();
	}
}
