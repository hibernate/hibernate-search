/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;

import org.apache.lucene.search.Query;

class LuceneUserProvidedLuceneQueryPredicate implements LuceneSearchPredicate {

	private final Set<String> indexNames;
	private final Query luceneQuery;

	LuceneUserProvidedLuceneQueryPredicate(LuceneSearchIndexScope<?> scope, Query luceneQuery) {
		this.indexNames = scope.hibernateSearchIndexNames();
		this.luceneQuery = luceneQuery;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
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
