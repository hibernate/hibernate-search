/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
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

	private LuceneNumericRangePredicate(Builder builder) {
		super( builder );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory<RangePredicateBuilder, F, AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public Builder<F, E> create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			return new Builder<>( codec, searchContext, field );
		}
	}

	private static class Builder<F, E extends Number> extends AbstractBuilder<F>
			implements RangePredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private Range<E> range;

		Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchContext searchContext,
				LuceneSearchValueFieldContext<F> field) {
			super( searchContext, field );
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
