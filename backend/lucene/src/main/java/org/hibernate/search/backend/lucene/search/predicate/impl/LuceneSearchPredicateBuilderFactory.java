/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchNonePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import org.apache.lucene.search.Query;

public class LuceneSearchPredicateBuilderFactory implements SearchPredicateBuilderFactory {

	private final LuceneSearchIndexScope<?> scope;

	public LuceneSearchPredicateBuilderFactory(LuceneSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new LuceneMatchAllPredicate.Builder( scope );
	}

	@Override
	public MatchNonePredicateBuilder matchNone() {
		return new LuceneMatchNonePredicate.Builder( scope );
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
	public QueryStringPredicateBuilder queryString() {
		return new LuceneQueryStringPredicate.Builder( scope );
	}

	public LuceneSearchPredicate fromLuceneQuery(Query query) {
		return new LuceneUserProvidedLuceneQueryPredicate( scope, query );
	}
}
