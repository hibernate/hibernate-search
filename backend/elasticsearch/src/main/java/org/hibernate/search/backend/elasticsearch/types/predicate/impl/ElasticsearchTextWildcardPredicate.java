package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.AbstractElasticsearchSingleFieldPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchTextWildcardPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor WILDCARD_ACCESSOR =
			JsonAccessor.root().property( "wildcard" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<JsonNode> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final JsonNode pattern;

	public ElasticsearchTextWildcardPredicate(Builder builder) {
		super( builder );
		this.pattern = builder.pattern;
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject, ObjectNode innerObject) {
		VALUE_ACCESSOR.set( innerObject, pattern );
		ObjectNode middleObject = new ObjectNode( JsonNodeFactory.instance );
		middleObject.set( absoluteFieldPath, innerObject );
		WILDCARD_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<WildcardPredicateBuilder, String> {

		@Override
		public WildcardPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements WildcardPredicateBuilder {

		private JsonNode pattern;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		public void pattern(String pattern) {
			this.pattern = JsonScalarConversions.toNode( pattern );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextWildcardPredicate( this );
		}
	}
}
