/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

class LuceneBooleanMatchPredicateBuilder extends AbstractLuceneMatchPredicateBuilder<Boolean> {

	private final LuceneBooleanFieldCodec codec;

	LuceneBooleanMatchPredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends Boolean> converter,
			LuceneBooleanFieldCodec codec) {
		super( searchContext, absoluteFieldPath, converter );
		this.codec = codec;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LongPoint.newExactQuery( absoluteFieldPath, codec.encode( value ) );
	}
}
