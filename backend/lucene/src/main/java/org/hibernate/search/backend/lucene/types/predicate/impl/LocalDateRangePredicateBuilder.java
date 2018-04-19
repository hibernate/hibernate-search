/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractRangePredicateBuilder;
import org.hibernate.search.backend.lucene.types.formatter.impl.LocalDateFieldFormatter;

class LocalDateRangePredicateBuilder extends AbstractRangePredicateBuilder<Long> {

	LocalDateRangePredicateBuilder(String absoluteFieldPath, LocalDateFieldFormatter formatter) {
		super( absoluteFieldPath, formatter );
	}

	@Override
	protected Query buildQuery() {
		return LongPoint.newRangeQuery(
				absoluteFieldPath,
				getLowerValue( lowerLimit, excludeLowerLimit ),
				getUpperValue( upperLimit, excludeUpperLimit )
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
