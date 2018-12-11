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

public final class LuceneLongFieldPredicateBuilderFactory<F>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, LuceneStandardFieldCodec<F, Long>> {

	public LuceneLongFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends F> converter,
			LuceneStandardFieldCodec<F, Long> codec) {
		super( converter, codec );
	}

	@Override
	public LuceneLongMatchPredicateBuilder<?> createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLongMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	public LuceneLongRangePredicateBuilder<?> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLongRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, codec );
	}
}
