/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

/**
 * A factory for search predicate builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search predicates.
 */
public interface SearchPredicateBuilderFactory {

	MatchAllPredicateBuilder matchAll();

	MatchNonePredicateBuilder matchNone();

	MatchIdPredicateBuilder id();

	BooleanPredicateBuilder bool();

	SimpleQueryStringPredicateBuilder simpleQueryString();

	QueryStringPredicateBuilder queryString();
}
