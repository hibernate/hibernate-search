/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneBooleanFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<Boolean, LuceneBooleanFieldCodec> {

	public LuceneBooleanFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends Boolean> converter,
			LuceneBooleanFieldCodec codec) {
		super( converter, codec );
	}

	@Override
	public LuceneBooleanMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneBooleanMatchPredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	public LuceneBooleanRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneBooleanRangePredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}
}
