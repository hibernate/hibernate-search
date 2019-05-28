/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.AbstractLuceneNumericFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;

/**
 * A predicate factory for fields encoded as a number.
 *
 * @param <F> The field type.
 * @param <E> The encoded number type.
 */
public final class LuceneNumericFieldPredicateBuilderFactory<F, E extends Number>
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<F, AbstractLuceneNumericFieldCodec<F, E>> {

	public LuceneNumericFieldPredicateBuilderFactory( boolean searchable,
			ToDocumentFieldValueConverter<?, ? extends F> converter, ToDocumentFieldValueConverter<F, ? extends F> rawConverter,
			AbstractLuceneNumericFieldCodec<F, E> codec) {
		super( searchable, converter, rawConverter, codec );
	}

	@Override
	public MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(LuceneSearchContext searchContext, String absoluteFieldPath,
			LuceneCompatibilityChecker converterChecker, LuceneCompatibilityChecker analyzerChecker) {
		checkSearchable( absoluteFieldPath );
		return new LuceneNumericMatchPredicateBuilder<>( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
	}

	@Override
	public LuceneNumericRangePredicateBuilder<F, E> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, LuceneCompatibilityChecker converterChecker) {
		checkSearchable( absoluteFieldPath );
		return new LuceneNumericRangePredicateBuilder<>( searchContext, absoluteFieldPath, converter, rawConverter, converterChecker, codec );
	}
}
