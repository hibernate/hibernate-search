/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;
import org.hibernate.search.engine.search.common.NamedValues;

import com.google.gson.JsonObject;

public class ElasticsearchWithParametersAggregation<A> extends AbstractElasticsearchAggregation<A> {
	private final ElasticsearchSearchIndexScope<?> scope;
	private final Function<? super NamedValues, ? extends AggregationFinalStep<A>> aggregationCreator;

	private ElasticsearchWithParametersAggregation(Builder<A> builder) {
		super( builder );
		scope = builder.scope;
		aggregationCreator = builder.aggregationCreator;
	}

	public Extractor<A> request(AggregationRequestContext context, AggregationKey<?> key, JsonObject jsonAggregations) {
		SearchAggregation<A> aggregation =
				aggregationCreator.apply( context.getRootPredicateContext().queryParameters() ).toAggregation();
		return ElasticsearchSearchAggregation.from( scope, aggregation )
				.request( context, key, jsonAggregations );
	}

	public static class Builder<T> extends AbstractBuilder<T>
			implements WithParametersAggregationBuilder<T> {

		private Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator;

		public Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public ElasticsearchWithParametersAggregation<T> build() {
			return new ElasticsearchWithParametersAggregation<>( this );
		}

		@Override
		public void creator(Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator) {
			this.aggregationCreator = aggregationCreator;
		}
	}
}
