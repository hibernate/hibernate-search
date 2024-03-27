/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchNonePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.QueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;

import com.google.gson.JsonObject;

public class ElasticsearchSearchPredicateBuilderFactory implements SearchPredicateBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;

	public ElasticsearchSearchPredicateBuilderFactory(ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new ElasticsearchMatchAllPredicate.Builder( scope );
	}

	@Override
	public MatchNonePredicateBuilder matchNone() {
		return new ElasticsearchMatchNonePredicate.Builder( scope );
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new ElasticsearchMatchIdPredicate.Builder( scope );
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new ElasticsearchBooleanPredicate.Builder( scope );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new ElasticsearchSimpleQueryStringPredicate.Builder( scope );
	}

	@Override
	public QueryStringPredicateBuilder queryString() {
		return new ElasticsearchQueryStringPredicate.Builder( scope );
	}

	public ElasticsearchSearchPredicate fromJson(JsonObject jsonObject) {
		return new ElasticsearchUserProvidedJsonPredicate( scope, jsonObject );
	}

	public ElasticsearchSearchPredicate fromJson(String jsonString) {
		return fromJson( scope.userFacingGson().fromJson( jsonString, JsonObject.class ) );
	}
}
