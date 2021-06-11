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
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.search.impl.AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;

public class LuceneTextTermsPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneTextTermsPredicate(Builder<?> builder) {
		super( builder );
	}

	public static class Factory<F>
			extends
			AbstractLuceneCodecAwareSearchValueFieldQueryElementFactory<TermsPredicateBuilder, F, LuceneStandardFieldCodec<F, String>> {
		public Factory(LuceneStandardFieldCodec<F, String> codec) {
			super( codec );
		}

		@Override
		public Builder<F> create(LuceneSearchIndexScope scope, LuceneSearchValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder<F> implements TermsPredicateBuilder {
		private final LuceneStandardFieldCodec<F, String> codec;

		private String term;
		private List<String> terms;
		private boolean allMatch;

		private Builder(LuceneStandardFieldCodec<F, String> codec, LuceneSearchIndexScope scope,
				LuceneSearchValueFieldContext<F> field) {
			super( scope, field );
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
			return new LuceneTextTermsPredicate( this );
		}

		@Override
		protected Query buildQuery() {
			if ( term != null ) {
				return new TermQuery( new Term( absoluteFieldPath, term ) );
			}

			if ( !allMatch ) {
				List<BytesRef> bytesRefs = terms.stream().map( BytesRef::new ).collect( Collectors.toList() );
				return new TermInSetQuery( absoluteFieldPath, bytesRefs );
			}

			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			for ( String termItem : terms ) {
				Query query = new TermQuery( new Term( absoluteFieldPath, termItem ) );
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