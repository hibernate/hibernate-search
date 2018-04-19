/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneFieldQueryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.MatchQueryOptions;
import org.hibernate.search.backend.lucene.document.model.impl.RangeQueryOptions;

public final class IntegerFieldQueryFactory implements LuceneFieldQueryFactory {

	public static final IntegerFieldQueryFactory INSTANCE = new IntegerFieldQueryFactory();

	private IntegerFieldQueryFactory() {
	}

	@Override
	public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
		return IntPoint.newExactQuery( fieldName, (Integer) value );
	}

	@Override
	public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
		return IntPoint.newRangeQuery(
				fieldName,
				getLowerValue( lowerLimit, rangeQueryOptions.isExcludeLowerLimit() ),
				getUpperValue( upperLimit, rangeQueryOptions.isExcludeUpperLimit() )
		);
	}

	private static int getLowerValue(Object lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return Integer.MIN_VALUE;
		}
		else {
			return excludeLowerLimit ? Math.addExact( (int) lowerLimit, 1 ) : (int) lowerLimit;
		}
	}

	private static int getUpperValue(Object upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return Integer.MAX_VALUE;
		}
		else {
			return excludeUpperLimit ? Math.addExact( (int) upperLimit, -1 ) : (int) upperLimit;
		}
	}
}
