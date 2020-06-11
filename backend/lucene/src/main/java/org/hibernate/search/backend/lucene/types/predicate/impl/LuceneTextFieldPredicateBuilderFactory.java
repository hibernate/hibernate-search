/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
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
	public LuceneTextMatchPredicateBuilder<?> createMatchPredicateBuilder(LuceneSearchContext searchContext,
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextMatchPredicateBuilder<>( searchContext, field, codec );
	}

	@Override
	public RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextRangePredicateBuilder<>( searchContext, field, codec );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextPhrasePredicateBuilder( searchContext, field, codec );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneTextWildcardPredicateBuilder( field );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			LuceneSearchFieldContext<F> field) {
		checkSearchable( field );
		return new LuceneSimpleQueryStringPredicateBuilderFieldState( field );
	}

}
