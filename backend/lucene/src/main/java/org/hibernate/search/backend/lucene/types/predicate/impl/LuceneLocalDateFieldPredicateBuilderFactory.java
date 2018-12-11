/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.time.LocalDate;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneLocalDateFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<LocalDate, LuceneLocalDateFieldCodec> {

	public LuceneLocalDateFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends LocalDate> converter,
			LuceneLocalDateFieldCodec codec) {
		super( converter, codec );
	}

	@Override
	public LuceneLocalDateMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLocalDateMatchPredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	public LuceneLocalDateRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLocalDateRangePredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}
}
