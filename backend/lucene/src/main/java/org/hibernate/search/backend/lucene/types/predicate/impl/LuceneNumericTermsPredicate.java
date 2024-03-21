/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameterCollectionOrSingle;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneLeafSingleFieldPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class LuceneNumericTermsPredicate extends AbstractLuceneLeafSingleFieldPredicate {

	private LuceneNumericTermsPredicate(Builder<?, ?> builder) {
		super( builder );
	}

	public static class Factory<F, E extends Number>
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<TermsPredicateBuilder, F, AbstractLuceneNumericFieldCodec<F, E>> {
		public Factory(AbstractLuceneNumericFieldCodec<F, E> codec) {
			super( codec );
		}

		@Override
		public Builder<F, E> create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( codec, scope, field );
		}
	}

	private static class Builder<F, E extends Number> extends AbstractBuilder<F> implements TermsPredicateBuilder {
		private final AbstractLuceneNumericFieldCodec<F, E> codec;

		private QueryParametersValueProvider<List<E>> termProvider;
		private boolean allMatch;

		private Builder(AbstractLuceneNumericFieldCodec<F, E> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<F> field) {
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
		public void matchingAnyParam(String parameterName, ValueConvert convert) {
			allMatch = false;
			fillTermParams( parameterName, convert );
		}

		@Override
		public void matchingAllParam(String parameterName, ValueConvert convert) {
			allMatch = true;
			fillTermParams( parameterName, convert );
		}

		@Override
		public SearchPredicate build() {
			return new LuceneNumericTermsPredicate( this );
		}

		@Override
		protected Query buildQuery(PredicateRequestContext context) {
			List<E> terms = termProvider.provide( context.toQueryParametersContext() );
			if ( terms.size() == 1 ) {
				return codec.getDomain().createExactQuery( absoluteFieldPath, terms.get( 0 ) );
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
			this.termProvider = simple( terms.stream()
					.map( term -> convertAndEncode( scope, field, codec, term, convert ) )
					.collect( Collectors.toList() ) );
		}

		private void fillTermParams(String parameterName, ValueConvert convert) {
			this.termProvider = parameterCollectionOrSingle( parameterName,
					terms -> convertAndEncode( terms, scope, field, codec, convert ) );
		}

		private static <T, R> List<R> convertAndEncode(Collection<?> terms, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<T> field,
				LuceneStandardFieldCodec<T, R> codec, ValueConvert convert) {
			return terms.stream()
					.map( term -> convertAndEncode( scope, field, codec, term, convert ) )
					.collect( Collectors.toList() );
		}
	}
}
