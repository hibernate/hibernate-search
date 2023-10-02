/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import com.google.gson.JsonObject;

public abstract class AbstractElasticsearchPredicate implements ElasticsearchSearchPredicate {

	private static final JsonAccessor<Float> BOOST_ACCESSOR = JsonAccessor.root().property( "boost" ).asFloat();

	private final Set<String> indexNames;
	// NOTE: below modifiers (boost, constant score) are used to implement hasNoModifiers() that other predicates
	// rely on and might build on to include additional predicate-specific modifiers ElasticsearchBooleanPredicate in particular.
	// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
	private final Float boost;
	private final boolean withConstantScore;

	protected AbstractElasticsearchPredicate(AbstractBuilder builder) {
		indexNames = builder.scope.hibernateSearchIndexNames();
		boost = builder.boost;
		withConstantScore = builder.withConstantScore;
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	@Override
	public JsonObject toJsonQuery(PredicateRequestContext context) {
		JsonObject outerObject = new JsonObject();
		JsonObject innerObject = new JsonObject();

		// in case of withConstantScore boots is set by constant_score clause
		if ( boost != null && !withConstantScore ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		JsonObject result = doToJsonQuery( context, outerObject, innerObject );
		return ( withConstantScore && result != null ) ? applyConstantScore( result ) : result;
	}

	protected abstract JsonObject doToJsonQuery(PredicateRequestContext context,
			JsonObject outerObject, JsonObject innerObject);

	protected boolean hasNoModifiers() {
		return !withConstantScore && boost == null;
	}

	private JsonObject applyConstantScore(JsonObject filter) {
		JsonObject constantScore = new JsonObject();
		constantScore.add( "filter", filter );
		if ( boost != null ) {
			BOOST_ACCESSOR.set( constantScore, boost );
		}

		JsonObject result = new JsonObject();
		result.add( "constant_score", constantScore );

		return result;
	}

	protected abstract static class AbstractBuilder implements SearchPredicateBuilder {
		protected final ElasticsearchSearchIndexScope<?> scope;

		// NOTE: below modifiers (boost, constant score) are used to implement hasNoModifiers() that other predicates
		// rely on and might build on to include additional predicate-specific modifiers ElasticsearchBooleanPredicate in particular.
		// IMPORTANT: Review where current modifiers are used and how the new modifier affects that logic, when adding a new modifier.
		private Float boost;
		private boolean withConstantScore = false;

		AbstractBuilder(ElasticsearchSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		@Override
		public void boost(float boost) {
			this.boost = boost;
		}

		@Override
		public void constantScore() {
			this.withConstantScore = true;
		}

		protected boolean hasNoModifiers() {
			return !withConstantScore && boost == null;
		}
	}
}
