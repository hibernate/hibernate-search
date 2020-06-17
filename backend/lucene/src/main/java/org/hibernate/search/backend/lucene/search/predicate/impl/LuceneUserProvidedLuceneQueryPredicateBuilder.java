/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;


class LuceneUserProvidedLuceneQueryPredicateBuilder implements LuceneSearchPredicateBuilder {

	private final LuceneSearchContext searchContext;
	private final Query luceneQuery;

	LuceneUserProvidedLuceneQueryPredicateBuilder(LuceneSearchContext searchContext, Query luceneQuery) {
		this.searchContext = searchContext;
		this.luceneQuery = luceneQuery;
	}

	@Override
	public SearchPredicate build() {
		return LuceneSearchPredicate.of( searchContext, this );
	}

	@Override
	public void checkNestableWithin(String expectedParentNestedPath) {
		// Nothing to do: we'll assume the user knows what they are doing.
	}

	@Override
	public Query toQuery(PredicateRequestContext context) {
		return luceneQuery;
	}
}
