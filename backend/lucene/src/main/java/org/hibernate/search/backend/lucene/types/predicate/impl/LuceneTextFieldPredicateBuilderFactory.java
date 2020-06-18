/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public final class LuceneTextFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneTextFieldCodec<F>> {

	public LuceneTextFieldPredicateBuilderFactory(boolean searchable, LuceneTextFieldCodec<F> codec) {
		super( searchable, codec );
	}

	@Override
	public LuceneTextMatchPredicate.Builder<?> createMatchPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextMatchPredicate.Builder<>( searchContext, field, codec );
	}

	@Override
	public RangePredicateBuilder createRangePredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextRangePredicate.Builder<>( searchContext, field, codec );
	}

	@Override
	public PhrasePredicateBuilder createPhrasePredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextPhrasePredicate.Builder<>( searchContext, field );
	}

	@Override
	public WildcardPredicateBuilder createWildcardPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextWildcardPredicate.Builder<>( searchContext, field );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneSimpleQueryStringPredicateBuilderFieldState( field );
	}

}
