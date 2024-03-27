/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.search.Query;

public class LuceneNumericMatchPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private LuceneNumericMatchPredicate(Builder<?, ?> builder) {
		super( builder );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<MatchPredicateBuilder, F, AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public Builder<F, E> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F, E extends Number> extends AbstractBuilder<F> implements MatchPredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private E value;

		private Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@Override
		public void value(Object value, ValueConvert convert) {
			this.value = convertAndEncode( codec, value, convert );
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			throw log.fullTextFeaturesNotSupportedByFieldType( field.eventContext() );
		}

		@Override
		public void analyzer(String analyzerName) {
			throw log.fullTextFeaturesNotSupportedByFieldType( field.eventContext() );
		}

		@Override
		public void skipAnalysis() {
			throw log.fullTextFeaturesNotSupportedByFieldType( field.eventContext() );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericMatchPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			return codec.getDomain().createExactQuery( absoluteFieldPath, value );
		}
	}
}
