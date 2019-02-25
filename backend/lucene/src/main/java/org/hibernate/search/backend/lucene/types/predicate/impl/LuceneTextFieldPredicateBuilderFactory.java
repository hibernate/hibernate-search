/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.Objects;

import org.apache.lucene.util.QueryBuilder;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

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
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other) {
		if ( !super.isDslCompatibleWith( other ) ) {
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
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneTextMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec, queryBuilder );
	}

	@Override
	public LuceneTextRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneTextRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}
}
