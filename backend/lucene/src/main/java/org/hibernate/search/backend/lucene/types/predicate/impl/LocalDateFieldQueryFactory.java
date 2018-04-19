/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.time.LocalDate;
import java.util.Objects;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.MatchQueryOptions;
import org.hibernate.search.backend.lucene.document.model.impl.RangeQueryOptions;
import org.hibernate.search.backend.lucene.types.formatter.impl.LocalDateFieldFormatter;

public final class LocalDateFieldQueryFactory implements LuceneFieldQueryFactory {

	private final LocalDateFieldFormatter localDateFieldFormatter;

	public LocalDateFieldQueryFactory(LocalDateFieldFormatter localDateFieldFormatter) {
		this.localDateFieldFormatter = localDateFieldFormatter;
	}

	@Override
	public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
		return LongPoint.newExactQuery( fieldName, ((LocalDate) value).toEpochDay() );
	}

	@Override
	public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
		return LongPoint.newRangeQuery(
				fieldName,
				getLowerValue( lowerLimit, rangeQueryOptions.isExcludeLowerLimit() ),
				getUpperValue( upperLimit, rangeQueryOptions.isExcludeUpperLimit() )
		);
	}

	private long getLowerValue(Object lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return excludeLowerLimit ? Math.addExact( Long.MIN_VALUE, 1 ) : Long.MIN_VALUE;
		}
		else {
			long lowerLimitAsLong = (long) localDateFieldFormatter.format( lowerLimit );
			return excludeLowerLimit ? Math.addExact( lowerLimitAsLong, 1 ) : lowerLimitAsLong;
		}
	}

	private long getUpperValue(Object upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return excludeUpperLimit ? Math.addExact( Long.MAX_VALUE, -1 ) : Long.MAX_VALUE;
		}
		else {
			long upperLimitAsLong = (long) localDateFieldFormatter.format( upperLimit );
			return excludeUpperLimit ? Math.addExact( upperLimitAsLong, -1 ) : upperLimitAsLong;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( LocalDateFieldQueryFactory.class != obj.getClass() ) {
			return false;
		}

		LocalDateFieldQueryFactory other = (LocalDateFieldQueryFactory) obj;

		return Objects.equals( localDateFieldFormatter, other.localDateFieldFormatter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( localDateFieldFormatter );
	}
}
