/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.formatter.impl.LocalDateFieldFormatter;

class LocalDateMatchPredicateBuilder extends AbstractMatchPredicateBuilder<Long> {

	LocalDateMatchPredicateBuilder(String absoluteFieldPath, LocalDateFieldFormatter formatter) {
		super( absoluteFieldPath, formatter );
	}

	@Override
	protected Query buildQuery() {
		return LongPoint.newExactQuery( absoluteFieldPath, value );
	}
}
