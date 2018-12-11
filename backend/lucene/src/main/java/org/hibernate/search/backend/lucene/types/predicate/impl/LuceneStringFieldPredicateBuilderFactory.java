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
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneStringFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneTextFieldCodec<F>> {

	private final QueryBuilder queryBuilder;

	public LuceneStringFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneTextFieldCodec<F> codec,
			QueryBuilder queryBuilder) {
		super( converter, codec );
		this.queryBuilder = queryBuilder;
	}

	@Override
	public boolean isDslCompatibleWith(LuceneFieldPredicateBuilderFactory other) {
		if ( !super.isDslCompatibleWith( other ) ) {
			return false;
		}
		LuceneStringFieldPredicateBuilderFactory<?> castedOther = (LuceneStringFieldPredicateBuilderFactory<?>) other;
		return Objects.equals( queryBuilder, castedOther.queryBuilder );
	}

	@Override
	public LuceneStringMatchPredicateBuilder<?> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneStringMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec, queryBuilder );
	}

	@Override
	public LuceneStringRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneStringRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}
}
