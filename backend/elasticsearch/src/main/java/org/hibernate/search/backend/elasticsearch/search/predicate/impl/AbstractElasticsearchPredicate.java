package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public abstract class AbstractElasticsearchPredicate implements ElasticsearchSearchPredicate {

	private static final JsonAccessor<Float> BOOST_ACCESSOR =
			JsonAccessor.root().property( "boost" ).unwrapSingletonArrays().asFloat();

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
	public ObjectNode toJsonQuery(PredicateRequestContext context) {
		ObjectNode outerObject = JsonNodeFactory.instance.objectNode();
		ObjectNode innerObject = JsonNodeFactory.instance.objectNode();

		// in case of withConstantScore boots is set by constant_score clause
		if ( boost != null && !withConstantScore ) {
			BOOST_ACCESSOR.set( innerObject, boost );
		}

		ObjectNode result = doToJsonQuery( context, outerObject, innerObject );
		return ( withConstantScore && result != null ) ? applyConstantScore( result ) : result;
	}

	protected abstract ObjectNode doToJsonQuery(PredicateRequestContext context,
			ObjectNode outerObject, ObjectNode innerObject);

	protected boolean hasNoModifiers() {
		return !withConstantScore && boost == null;
	}

	private ObjectNode applyConstantScore(ObjectNode filter) {
		ObjectNode constantScore = JsonNodeFactory.instance.objectNode();
		constantScore.set( "filter", filter );
		if ( boost != null ) {
			BOOST_ACCESSOR.set( constantScore, boost );
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set( "constant_score", constantScore );

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
