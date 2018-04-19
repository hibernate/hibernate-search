/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractRangePredicateBuilder;
import org.hibernate.search.backend.lucene.types.formatter.impl.IntegerFieldFormatter;

class IntegerRangePredicateBuilder extends AbstractRangePredicateBuilder<Integer> {

	IntegerRangePredicateBuilder(String absoluteFieldPath, IntegerFieldFormatter formatter) {
		super( absoluteFieldPath, formatter );
	}

	@Override
	protected Query buildQuery() {
		return IntPoint.newRangeQuery(
				absoluteFieldPath,
				getLowerValue( lowerLimit, excludeLowerLimit ),
				getUpperValue( upperLimit, excludeUpperLimit )
		);
	}

	private static int getLowerValue(Integer lowerLimit, boolean excludeLowerLimit) {
		if ( lowerLimit == null ) {
			return Integer.MIN_VALUE;
		}
		else {
			return excludeLowerLimit ? Math.addExact( lowerLimit, 1 ) : lowerLimit;
		}
	}

	private static int getUpperValue(Integer upperLimit, boolean excludeUpperLimit) {
		if ( upperLimit == null ) {
			return Integer.MAX_VALUE;
		}
		else {
			return excludeUpperLimit ? Math.addExact( upperLimit, -1 ) : upperLimit;
		}
	}
}
