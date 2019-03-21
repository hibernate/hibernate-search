/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.apache.lucene.util.QueryBuilder;

import org.hibernate.search.backend.lucene.search.impl.LuceneConverterCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public final class LuceneTextFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneTextFieldCodec<F>> {

	private final QueryBuilder queryBuilder;

	public LuceneTextFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			LuceneTextFieldCodec<F> codec,
			QueryBuilder queryBuilder) {
		super( converter, rawConverter, codec );
		this.queryBuilder = queryBuilder;
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other, DslConverter dslConverter) {
		if ( !super.isDslCompatibleWith( other, dslConverter ) ) {
			return false;
		}
		LuceneTextFieldPredicateBuilderFactory<?> castedOther = (LuceneTextFieldPredicateBuilderFactory<?>) other;
		if ( queryBuilder == null || castedOther == null ) {
			return queryBuilder == null && castedOther.queryBuilder == null;
		}
		else {
			return Objects.equals( queryBuilder.getAnalyzer(), castedOther.queryBuilder.getAnalyzer() );
		}
	}

	@Override
	public LuceneTextMatchPredicateBuilder<?> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, LuceneConverterCompatibilityChecker converterChecker) {
		return new LuceneTextMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec, queryBuilder );
	}

	@Override
	public LuceneTextRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, DslConverter dslConverter) {
		return new LuceneTextRangePredicateBuilder<>( searchContext, absoluteFieldPath, getConverter( dslConverter ), codec );
	}

	@Override
	public PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(String absoluteFieldPath) {
		return new LuceneTextPhrasePredicateBuilder( absoluteFieldPath, codec, queryBuilder );
	}

	@Override
	public WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath) {
		return new LuceneTextWildcardPredicateBuilder( absoluteFieldPath );
	}

	@Override
	public LuceneSimpleQueryStringPredicateBuilderFieldContext createSimpleQueryStringFieldContext(
			String absoluteFieldPath) {
		return new LuceneSimpleQueryStringPredicateBuilderFieldContext(
				queryBuilder == null ? null : queryBuilder.getAnalyzer()
		);
	}
}
