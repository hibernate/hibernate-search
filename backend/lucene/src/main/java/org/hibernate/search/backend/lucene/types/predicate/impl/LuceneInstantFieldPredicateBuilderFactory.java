/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.time.Instant;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneInstantFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneInstantFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<Instant, LuceneInstantFieldCodec> {

	public LuceneInstantFieldPredicateBuilderFactory(
			ToDocumentFieldValueConverter<?, ? extends Instant> converter,
			LuceneInstantFieldCodec codec) {
		super( converter, codec );
	}

	@Override
	public LuceneInstantMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneInstantMatchPredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}

	@Override
	public LuceneInstantRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneInstantRangePredicateBuilder( searchContext, absoluteFieldPath, converter, codec );
	}
}
