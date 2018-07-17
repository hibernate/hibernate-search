/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.search.SearchPredicate;

import org.apache.lucene.search.Query;

class LuceneSearchPredicate implements SearchPredicate, LuceneSearchPredicateBuilder {

	private final LuceneSearchPredicateBuilder delegate;

	LuceneSearchPredicate(LuceneSearchPredicateBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public Query build(LuceneSearchPredicateContext context) {
		return delegate.build( context );
	}

}
