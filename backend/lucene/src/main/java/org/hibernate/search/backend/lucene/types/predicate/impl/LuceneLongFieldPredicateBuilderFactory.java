/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLongFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneLongFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<Long, LuceneLongFieldCodec> {

	public LuceneLongFieldPredicateBuilderFactory(ToDocumentFieldValueConverter<?, ? extends Long> converter,
			LuceneLongFieldCodec codec) {
		super( converter, codec );
	}

	@Override
	public LuceneLongMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLongMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public LuceneLongRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneLongRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
