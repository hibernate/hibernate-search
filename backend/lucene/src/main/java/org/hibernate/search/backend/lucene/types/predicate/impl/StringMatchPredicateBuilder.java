/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractMatchPredicateBuilder;
import org.hibernate.search.backend.lucene.types.formatter.impl.StringFieldFormatter;

class StringMatchPredicateBuilder extends AbstractMatchPredicateBuilder<String> {

	private final StringFieldFormatter formatter;

	private final QueryBuilder queryBuilder;

	StringMatchPredicateBuilder(String absoluteFieldPath, StringFieldFormatter formatter, QueryBuilder queryBuilder) {
		super( absoluteFieldPath, formatter );
		this.formatter = formatter;
		this.queryBuilder = queryBuilder;
	}

	@Override
	protected Query buildQuery() {
		if ( queryBuilder != null ) {
			return queryBuilder.createBooleanQuery( absoluteFieldPath, value );
		}
		else {
			// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
			// the queryBuilder case above

			return new TermQuery( new Term( absoluteFieldPath, formatter.normalize( absoluteFieldPath, value ) ) );
		}
	}
}
