/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchNonePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

public class StubSearchPredicateBuilderFactory
		implements SearchPredicateBuilderFactory {

	@Override
	public MatchIdPredicateBuilder id() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public MatchNonePredicateBuilder matchNone() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new StubSearchPredicate.SimpleQueryStringBuilder();
	}

	@Override
	public QueryStringPredicateBuilder queryString() {
		return new StubSearchPredicate.QueryStringBuilder();
	}

}
