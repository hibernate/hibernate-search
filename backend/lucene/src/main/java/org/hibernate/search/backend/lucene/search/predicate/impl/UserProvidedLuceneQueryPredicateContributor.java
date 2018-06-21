/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;


class UserProvidedLuceneQueryPredicateContributor implements SearchPredicateContributor<LuceneSearchPredicateContext, LuceneSearchPredicateCollector> {

	private final Query luceneQuery;

	UserProvidedLuceneQueryPredicateContributor(Query luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	@Override
	public void contribute(LuceneSearchPredicateContext context, LuceneSearchPredicateCollector collector) {
		collector.collectPredicate( luceneQuery );
	}
}
