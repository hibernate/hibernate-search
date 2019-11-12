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

import com.google.gson.JsonObject;

final class ElasticsearchJsonAggregationFinalStep
		implements AggregationFinalStep<JsonObject> {
	private final SearchAggregationBuilder<JsonObject> builder;

	ElasticsearchJsonAggregationFinalStep(SearchAggregationBuilder<JsonObject> builder) {
		this.builder = builder;
	}

	@Override
	public SearchAggregation<JsonObject> toAggregation() {
		return builder.build();
	}
}
