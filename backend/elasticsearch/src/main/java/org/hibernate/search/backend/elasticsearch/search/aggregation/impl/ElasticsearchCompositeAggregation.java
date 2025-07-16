/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

import com.google.gson.JsonObject;

public class ElasticsearchCompositeAggregation<A> extends AbstractElasticsearchAggregation<A> {
	private final ElasticsearchSearchAggregation<?>[] aggregations;
	private final ResultsCompositor<?, A> compositor;


	private ElasticsearchCompositeAggregation(Builder<A> builder) {
		super( builder );
		aggregations = builder.inners;
		compositor = builder.compositor;
	}

	public Extractor<A> request(AggregationRequestContext context, AggregationKey<?> key, JsonObject jsonAggregations) {
		Extractor<?>[] extractors = new Extractor[aggregations.length];
		AggregationKey<?>[] keys = new AggregationKey[aggregations.length];
		for ( int i = 0; i < aggregations.length; i++ ) {
			keys[i] = AggregationKey.of( key.name() + "_composite_" + i );
			JsonObject innerObject = new JsonObject();
			extractors[i] = aggregations[i].request( context, keys[i], innerObject );
			if ( !innerObject.isEmpty() ) {
				jsonAggregations.add( keys[i].name(), innerObject.get( keys[i].name() ) );
			}
		}
		return new CompositeExtractor<>( key, compositor, extractors, keys );
	}

	public static class Builder<T> extends AbstractBuilder<T>
			implements CompositeAggregationBuilder<T> {
		private ElasticsearchSearchAggregation<?>[] inners;
		private ResultsCompositor<?, T> compositor;

		public Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchAggregation<?>[] inners,
				ResultsCompositor<?, T> compositor) {
			super( scope );
			this.inners = inners;
			this.compositor = compositor;
		}

		@Override
		public ElasticsearchCompositeAggregation<T> build() {
			return new ElasticsearchCompositeAggregation<>( this );
		}

		@Override
		public CompositeAggregationBuilder<T> innerAggregations(SearchAggregation<?>[] inners) {
			this.inners = new ElasticsearchSearchAggregation[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				this.inners[i] = ElasticsearchSearchAggregation.from( scope, inners[i] );
			}
			return this;
		}

		@Override
		public <V> CompositeAggregationBuilder<V> compositor(ResultsCompositor<?, V> compositor) {
			return new Builder<>( scope, inners, compositor );
		}
	}

	private record CompositeExtractor<E, A>(AggregationKey<?> key, ResultsCompositor<E, A> compositor,
											Extractor<?>[] extractors,
											AggregationKey<?>[] keys)
			implements Extractor<A> {

		@Override
		public A extract(JsonObject aggregationResult, AggregationExtractContext context) {
			E initial = compositor.createInitial();

			for ( int i = 0; i < extractors.length; i++ ) {
				initial = compositor.set( initial, i, extractors[i].extract( aggregationResult, context ) );
			}

			return compositor.finish( initial );
		}
	}
}
