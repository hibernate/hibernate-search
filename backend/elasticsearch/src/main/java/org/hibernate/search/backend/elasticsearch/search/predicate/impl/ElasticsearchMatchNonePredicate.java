package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchNonePredicateBuilder;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchMatchNonePredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor MATCH_NONE_ACCESSOR = JsonAccessor.root().property( "match_none" )
			.unwrapSingletonArrays().asObject();

	ElasticsearchMatchNonePredicate(AbstractBuilder builder) {
		super( builder );
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		// Nothing to do
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context,
			ObjectNode outerObject, ObjectNode innerObject) {
		MATCH_NONE_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	static class Builder extends AbstractBuilder implements MatchNonePredicateBuilder {
		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchMatchNonePredicate( this );
		}
	}
}
