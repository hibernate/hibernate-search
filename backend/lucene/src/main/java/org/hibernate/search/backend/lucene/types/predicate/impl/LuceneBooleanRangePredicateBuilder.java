/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneRangePredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneBooleanFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

class LuceneBooleanRangePredicateBuilder extends AbstractLuceneRangePredicateBuilder<Boolean> {

	private final LuceneBooleanFieldCodec codec;

	LuceneBooleanRangePredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends Boolean> converter,
			LuceneBooleanFieldCodec codec) {
		super( searchContext, absoluteFieldPath, converter );
		this.codec = codec;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return IntPoint.newRangeQuery(
				absoluteFieldPath,
				getLowerValue( codec.encode( lowerLimit ), excludeLowerLimit ),
				getUpperValue( codec.encode( upperLimit ), excludeUpperLimit )
		);
	}

	private int getLowerValue(Integer lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return excludeLowerLimit ? Math.addExact( Integer.MIN_VALUE, 1 ) : Integer.MIN_VALUE;
		}
		else {
			return excludeLowerLimit ? Math.addExact( lowerLimit, 1 ) : lowerLimit;
		}
	}

	private int getUpperValue(Integer upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return excludeUpperLimit ? Math.addExact( Integer.MAX_VALUE, -1 ) : Integer.MAX_VALUE;
		}
		else {
			return excludeUpperLimit ? Math.addExact( upperLimit, -1 ) : upperLimit;
		}
	}
}
