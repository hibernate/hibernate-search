/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.util.Optional;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.lowlevel.impl.LuceneNumericDomain;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
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

		private QueryParametersValueProvider<Range<E>> rangeProvider;

		Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			this.codec = codec;
		}

		@Override
		public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
			this.rangeProvider = simple( convertAndEncode( scope, field, codec, range, convertLowerBound, convertUpperBound ) );
		}

		@Override
		public void param(String parameterName, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert) {
			this.rangeProvider = parameter( parameterName, Range.class,
					range -> convertAndEncode( scope, field, codec, range, lowerBoundConvert, upperBoundConvert ) );
		}

		@Override
		public void parameterized(Range<String> range, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert) {
			this.rangeProvider = context -> convertAndEncode(
					scope, field, codec, range.map( context::parameter ), lowerBoundConvert, upperBoundConvert
			);
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericRangePredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			LuceneNumericDomain<E> domain = codec.getDomain();
			Range<E> range = rangeProvider.provide( context.toQueryParametersContext() );
			return domain.createRangeQuery(
					absoluteFieldPath,
					getLowerValue( domain, range.lowerBoundValue(), range.lowerBoundInclusion() ),
					getUpperValue( domain, range.upperBoundValue(), range.upperBoundInclusion() )
			);
		}

		private static <E extends Number> E getLowerValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
				RangeBoundInclusion inclusion) {
			if ( boundValueOptional.isEmpty() ) {
				return domain.getMinValue();
			}
			E boundValue = boundValueOptional.get();
			return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getNextValue( boundValue ) : boundValue;
		}

		private static <E extends Number> E getUpperValue(LuceneNumericDomain<E> domain, Optional<E> boundValueOptional,
				RangeBoundInclusion inclusion) {
			if ( boundValueOptional.isEmpty() ) {
				return domain.getMaxValue();
			}
			E boundValue = boundValueOptional.get();
			return RangeBoundInclusion.EXCLUDED.equals( inclusion ) ? domain.getPreviousValue( boundValue ) : boundValue;
		}
	}
}
