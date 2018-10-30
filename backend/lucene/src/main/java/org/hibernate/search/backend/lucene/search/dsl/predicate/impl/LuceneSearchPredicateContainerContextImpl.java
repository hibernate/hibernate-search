/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate.impl;

import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateContainerContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateContainerContextImpl
		extends DelegatingSearchPredicateContainerContextImpl
		implements LuceneSearchPredicateContainerContext {

	private final LuceneSearchPredicateFactory factory;

	public LuceneSearchPredicateContainerContextImpl(SearchPredicateContainerContext delegate,
			LuceneSearchPredicateFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public SearchPredicateTerminalContext fromLuceneQuery(Query luceneQuery) {
		return new LuceneQueryPredicateContextImpl( factory, luceneQuery );
	}
}
