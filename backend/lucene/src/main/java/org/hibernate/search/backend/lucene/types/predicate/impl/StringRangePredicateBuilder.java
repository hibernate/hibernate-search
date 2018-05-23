/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractRangePredicateBuilder;
import org.hibernate.search.backend.lucene.types.formatter.impl.StringFieldFormatter;

class StringRangePredicateBuilder extends AbstractRangePredicateBuilder<String> {

	private final StringFieldFormatter stringFormatter;

	StringRangePredicateBuilder(String absoluteFieldPath, StringFieldFormatter stringFormatter) {
		super( absoluteFieldPath, stringFormatter );
		this.stringFormatter = stringFormatter;
	}

	@Override
	protected Query buildQuery() {
		// Note that a range query only makes sense if only one token is returned by the analyzer
		// and we should even consider forcing having a normalizer here, instead of supporting
		// range queries on analyzed fields.

		return TermRangeQuery.newStringRange(
				absoluteFieldPath,
				stringFormatter.normalize( absoluteFieldPath, lowerLimit ),
				stringFormatter.normalize( absoluteFieldPath, upperLimit ),
				// we force the true value if the limit is null because of some Lucene checks down the hill
				lowerLimit == null ? true : !excludeLowerLimit,
				upperLimit == null ? true : !excludeUpperLimit
		);
	}
}
