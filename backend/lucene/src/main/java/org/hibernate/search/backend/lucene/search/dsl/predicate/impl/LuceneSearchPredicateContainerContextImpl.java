/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.predicate.impl;

import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateContainerContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.DelegatingSearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateContainerContextImpl<N>
		extends DelegatingSearchPredicateContainerContextImpl<N>
		implements LuceneSearchPredicateContainerContext<N> {

	private final LuceneSearchPredicateFactory factory;

	private final SearchPredicateDslContext<N, ? super LuceneSearchPredicateBuilder> dslContext;

	public LuceneSearchPredicateContainerContextImpl(SearchPredicateContainerContext<N> delegate,
			LuceneSearchPredicateFactory factory,
			SearchPredicateDslContext<N, ? super LuceneSearchPredicateBuilder> dslContext) {
		super( delegate );
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public N fromLuceneQuery(Query luceneQuery) {
		dslContext.addChild( factory.fromLuceneQuery( luceneQuery ) );
		return dslContext.getNextContext();
	}
}
