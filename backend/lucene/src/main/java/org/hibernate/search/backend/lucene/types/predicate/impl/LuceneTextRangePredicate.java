/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

class LuceneTextRangePredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextRangePredicate(Builder builder) {
		super( builder );
	}

	static class Builder<F> extends AbstractBuilder<F> implements RangePredicateBuilder {
		private final LuceneTextFieldCodec<F> codec;

		private Range<String> range;

		Builder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
				LuceneTextFieldCodec<F> codec) {
			super( searchContext, field );
			this.codec = codec;
		}

		@Override
		public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
			this.range = convertAndEncode( codec, range, convertLowerBound, convertUpperBound );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneTextRangePredicate( this );
		}

		@Override
		protected Query buildQuery() {
			// Note that a range query only makes sense if only one token is returned by the analyzer
			// and we should even consider forcing having a normalizer here, instead of supporting
			// range queries on analyzed fields.

			return new TermRangeQuery(
					absoluteFieldPath,
					codec.normalize( absoluteFieldPath, range.lowerBoundValue().orElse( null ) ),
					codec.normalize( absoluteFieldPath, range.upperBoundValue().orElse( null ) ),
					// we force the true value if the bound is null because of some Lucene checks down the hill
					RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() )
							|| !range.lowerBoundValue().isPresent(),
					RangeBoundInclusion.INCLUDED.equals( range.upperBoundInclusion() )
							|| !range.upperBoundValue().isPresent()
			);
		}
	}
}
