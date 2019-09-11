/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

import org.apache.lucene.search.Query;

class LuceneSearchPredicate implements SearchPredicate, LuceneSearchPredicateBuilder {

	private final Set<String> indexNames;
	private final LuceneSearchPredicateBuilder delegate;

	LuceneSearchPredicate(Set<String> indexNames, LuceneSearchPredicateBuilder delegate) {
		this.indexNames = indexNames;
		this.delegate = delegate;
	}

	@Override
	public Query build(LuceneSearchPredicateContext context) {
		return delegate.build( context );
	}

	public Set<String> getIndexNames() {
		return indexNames;
	}
}
