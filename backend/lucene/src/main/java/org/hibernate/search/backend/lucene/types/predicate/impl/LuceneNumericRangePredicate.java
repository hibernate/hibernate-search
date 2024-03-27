/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.search.Query;

public class LuceneNumericRangePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneNumericRangePredicate(Builder<?, ?> builder) {
		super( builder );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<RangePredicateBuilder, F, AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public Builder<F, E> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F, E extends Number> extends AbstractBuilder<F>
			implements RangePredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private Range<E> range;

		Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@Override
		public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
			this.range = convertAndEncode( codec, range, convertLowerBound, convertUpperBound );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericRangePredicate( this );
		}

		@Override
		protected Query buildQuery() {
			LuceneNumericDomain<E> domain = codec.getDomain();
			return domain.createRangeQuery(
					absoluteFieldPath,
					getLowerValue( domain, range.lowerBoundValue(), range.lowerBoundInclusion() ),
					getUpperValue( domain, range.upperBoundValue(), range.upperBoundInclusion() )
			);
		}

		private static <E extends Number> E getLowerValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
				RangeBoundInclusion inclusion) {
			if ( !boundValueOptional.isPresent() ) {
				return domain.getMinValue();
			}
			E boundValue = boundValueOptional.get();
			return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getNextValue( boundValue ) : boundValue;
		}

		private static <E extends Number> E getUpperValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
				RangeBoundInclusion inclusion) {
			if ( !boundValueOptional.isPresent() ) {
				return domain.getMaxValue();
			}
			E boundValue = boundValueOptional.get();
			return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getPreviousValue( boundValue ) : boundValue;
		}
	}
}
