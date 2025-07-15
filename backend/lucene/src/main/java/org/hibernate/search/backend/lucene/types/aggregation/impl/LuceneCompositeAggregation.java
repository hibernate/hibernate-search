/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.CompositeAggregationBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public class LuceneCompositeAggregation<A> implements LuceneSearchAggregation<A> {
	private final LuceneSearchIndexScope<?> scope;
	private final LuceneSearchAggregation<?>[] aggregations;
	private final ResultsCompositor<?, A> compositor;

	private LuceneCompositeAggregation(Builder<A> builder) {
		this.scope = builder.scope;
		this.aggregations = builder.inners;
		this.compositor = builder.compositor;
	}

	@Override
	public Extractor<A> request(AggregationRequestContext context) {
		Extractor<?>[] extractors = new Extractor[aggregations.length];
		for ( int i = 0; i < aggregations.length; i++ ) {
			extractors[i] = aggregations[i].request( context );
		}
		return new CompositeExtractor<>( compositor, extractors );
	}

	@Override
	public Set<String> indexNames() {
		return scope.hibernateSearchIndexNames();
	}

	public static class Builder<A> implements CompositeAggregationBuilder<A> {

		private final LuceneSearchIndexScope<?> scope;
		private LuceneSearchAggregation<?>[] inners;
		private ResultsCompositor<?, A> compositor;

		public Builder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchAggregation<?>[] inners,
				ResultsCompositor<?, A> compositor) {
			this.scope = scope;
			this.inners = inners;
			this.compositor = compositor;
		}

		@Override
		public SearchAggregation<A> build() {
			return new LuceneCompositeAggregation<>( this );
		}

		@Override
		public CompositeAggregationBuilder<A> innerAggregations(SearchAggregation<?>[] inners) {
			this.inners = new LuceneSearchAggregation[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				this.inners[i] = LuceneSearchAggregation.from( scope, inners[i] );
			}
			return this;
		}

		@Override
		public <V> CompositeAggregationBuilder<V> compositor(ResultsCompositor<?, V> compositor) {
			return new Builder<>( scope, inners, compositor );
		}
	}

	private record CompositeExtractor<E, A>(ResultsCompositor<E, A> compositor, Extractor<?>[] extractors)
			implements Extractor<A> {

		@Override
		public A extract(AggregationExtractContext context) throws IOException {
			E initial = compositor.createInitial();

			for ( int i = 0; i < extractors.length; i++ ) {
				initial = compositor.set( initial, i, extractors[i].extract( context ) );
			}

			return compositor.finish( initial );
		}
	}
}
