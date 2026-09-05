/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.GsonUtils;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.DisjunctionMaxPredicateBuilder;

import com.google.gson.JsonObject;

class ElasticsearchDisjunctionMaxPredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor DIS_MAX_ACCESSOR = JsonAccessor.root().property( "dis_max" ).asObject();

	private static final String QUERIES_PROPERTY_NAME = "queries";

	private static final JsonAccessor<Float> TIE_BREAKER_ACCESSOR =
			JsonAccessor.root().property( "tie_breaker" ).asFloat();

	private final List<ElasticsearchSearchPredicate> disjuncts;
	private final Float tieBreaker;

	private ElasticsearchDisjunctionMaxPredicate(Builder builder) {
		super( builder );
		disjuncts = builder.disjuncts;
		tieBreaker = builder.tieBreaker;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.disjuncts = null;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		for ( ElasticsearchSearchPredicate disjunct : disjuncts ) {
			disjunct.checkNestableWithin( context );
		}
	}

	@Override
	protected JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject) {
		for ( ElasticsearchSearchPredicate disjunct : disjuncts ) {
			GsonUtils.setOrAppendToArray( innerObject, QUERIES_PROPERTY_NAME, disjunct.toJsonQuery( context ) );
		}

		if ( tieBreaker != null ) {
			TIE_BREAKER_ACCESSOR.set( innerObject, tieBreaker );
		}

		DIS_MAX_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder
			implements DisjunctionMaxPredicateBuilder {

		private List<ElasticsearchSearchPredicate> disjuncts = new ArrayList<>();
		private Float tieBreaker;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void disjunct(SearchPredicate clause) {
			ElasticsearchSearchPredicate elasticsearchClause = ElasticsearchSearchPredicate.from( scope, clause );
			disjuncts.add( elasticsearchClause );
		}

		@Override
		public void tieBreaker(float tieBreaker) {
			this.tieBreaker = tieBreaker;
		}

		@Override
		public boolean hasClause() {
			return !disjuncts.isEmpty();
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchDisjunctionMaxPredicate( this );
		}
	}
}
