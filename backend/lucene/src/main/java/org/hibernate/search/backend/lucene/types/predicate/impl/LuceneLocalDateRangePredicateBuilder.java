/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.time.LocalDate;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneRangePredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateFieldCodec;
import org.hibernate.search.engine.backend.document.converter.ToDocumentFieldValueConverter;

class LuceneLocalDateRangePredicateBuilder extends AbstractLuceneRangePredicateBuilder<LocalDate> {

	private final LuceneLocalDateFieldCodec codec;

	LuceneLocalDateRangePredicateBuilder(
			LuceneSearchContext searchContext,
			String absoluteFieldPath,
			ToDocumentFieldValueConverter<?, ? extends LocalDate> converter,
			LuceneLocalDateFieldCodec codec) {
		super( searchContext, absoluteFieldPath, converter );
		this.codec = codec;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return LongPoint.newRangeQuery(
				absoluteFieldPath,
				getLowerValue( codec.encode( lowerLimit ), excludeLowerLimit ),
				getUpperValue( codec.encode( upperLimit ), excludeUpperLimit )
		);
	}

	private long getLowerValue(Long lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return excludeLowerLimit ? Math.addExact( Long.MIN_VALUE, 1 ) : Long.MIN_VALUE;
		}
		else {
			return excludeLowerLimit ? Math.addExact( lowerLimit, 1 ) : lowerLimit;
		}
	}

	private long getUpperValue(Long upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return excludeUpperLimit ? Math.addExact( Long.MAX_VALUE, -1 ) : Long.MAX_VALUE;
		}
		else {
			return excludeUpperLimit ? Math.addExact( upperLimit, -1 ) : upperLimit;
		}
	}
}
