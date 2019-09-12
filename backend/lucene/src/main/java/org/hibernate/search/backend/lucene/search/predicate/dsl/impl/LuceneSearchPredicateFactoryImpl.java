/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.dsl.impl;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.DelegatingSearchPredicateFactory;

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateFactoryImpl
		extends DelegatingSearchPredicateFactory
		implements LuceneSearchPredicateFactory {

	private final LuceneSearchPredicateBuilderFactory factory;

	public LuceneSearchPredicateFactoryImpl(SearchPredicateFactory delegate,
			LuceneSearchPredicateBuilderFactory factory) {
		super( delegate );
		this.factory = factory;
	}

	@Override
	public PredicateFinalStep fromLuceneQuery(Query luceneQuery) {
		return new LuceneQueryPredicateFinalStep( factory, luceneQuery );
	}
}
