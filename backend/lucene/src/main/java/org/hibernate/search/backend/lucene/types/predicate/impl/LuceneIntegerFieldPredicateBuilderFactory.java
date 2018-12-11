/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneIntegerFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

public final class LuceneIntegerFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<Integer, LuceneIntegerFieldCodec> {

	public LuceneIntegerFieldPredicateBuilderFactory(
			ToDocumentFieldValueConverter<?, ? extends Integer> converter,
			LuceneIntegerFieldCodec codec) {
		super( converter, codec );
	}

	@Override
	public LuceneIntegerMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneIntegerMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public LuceneIntegerRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneIntegerRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}
