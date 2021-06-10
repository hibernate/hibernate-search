/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import org.apache.lucene.search.Query;


public class LuceneSearchPredicateBuilderFactoryImpl implements LuceneSearchPredicateBuilderFactory {

	private final LuceneSearchIndexScope scope;

	public LuceneSearchPredicateBuilderFactoryImpl(LuceneSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public void contribute(LuceneSearchPredicateCollector collector, SearchPredicate predicate) {
		LuceneSearchPredicate lucenePredicate = LuceneSearchPredicate.from( scope, predicate );
		collector.collectPredicate( lucenePredicate.toQuery( PredicateRequestContext.root() ) );
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new LuceneMatchAllPredicate.Builder( scope );
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new LuceneMatchIdPredicate.Builder( scope );
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new LuceneBooleanPredicate.Builder( scope );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new LuceneSimpleQueryStringPredicate.Builder( scope );
	}

	@Override
	public LuceneSearchPredicate fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicate( scope, query );
	}
}
