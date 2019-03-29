/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

public class LuceneExistsPredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements ExistsPredicateBuilder<LuceneSearchPredicateBuilder> {

	private final String absoluteFieldPath;

	LuceneExistsPredicateBuilder(String absoluteFieldPath) {
		this.absoluteFieldPath = absoluteFieldPath;
		// Score is always constant for this query
		withConstantScore();
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		return new TermQuery( new Term( LuceneFields.fieldNamesFieldName(), absoluteFieldPath ) );
	}

}
