/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class LuceneNumericTermsPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneNumericTermsPredicate(Builder<?, ?> builder) {
		super( builder );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory<TermsPredicateBuilder, F, AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public Builder<F, E> create(LuceneSearchContext searchContext, LuceneSearchValueFieldContext<F> field) {
			return new Builder<>( codec, searchContext, field );
		}
	}

	private static class Builder<F, E extends Number> extends AbstractBuilder<F> implements TermsPredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private E term;
		private List<E> terms;
		private boolean allMatch;

		private Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchContext searchContext,
				LuceneSearchValueFieldContext<F> field) {
			super( searchContext, field );
			// Score is always constant for this query
			constantScore();

			this.codec = codec;
		}

		@Override
		public void matchingAny(Collection<?> terms, ValueConvert convert) {
			allMatch = false;
			fillTerms( terms, convert );
		}

		@Override
		public void matchingAll(Collection<?> terms, ValueConvert convert) {
			allMatch = true;
			fillTerms( terms, convert );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericTermsPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			if ( term != null ) {
				return codec.getDomain().createExactQuery( absoluteFieldPath, term );
			}

			if ( !allMatch ) {
				return codec.getDomain().createSetQuery( absoluteFieldPath, terms );
			}

			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			for ( E termItem : terms ) {
				Query query = codec.getDomain().createExactQuery( absoluteFieldPath, termItem );
				builder.add( query, BooleanClause.Occur.MUST );
			}
			return builder.build();
		}

		private void fillTerms(Collection<?> terms, ValueConvert convert) {
			if ( terms.size() == 1 ) {
				this.term = convertAndEncode( codec, terms.iterator().next(), convert );
				this.terms = null;
				return;
			}

			this.term = null;
			this.terms = new ArrayList<>( terms.size() );
			for ( Object termItem : terms ) {
				this.terms.add( convertAndEncode( codec, termItem, convert ) );
			}
		}
	}
}
