/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStandardFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

/**
 * A predicate factory for fields encoded as an integer.
 *
 * @param <F> The field type.
 */
public final class LuceneIntegerFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneStandardFieldCodec<F, Integer>> {

	public LuceneIntegerFieldPredicateBuilderFactory(
			ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneStandardFieldCodec<F, Integer> codec) {
		super( converter, codec );
	}

	@Override
	public LuceneIntegerMatchPredicateBuilder<F> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneIntegerMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	public LuceneIntegerRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneIntegerRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}
}
