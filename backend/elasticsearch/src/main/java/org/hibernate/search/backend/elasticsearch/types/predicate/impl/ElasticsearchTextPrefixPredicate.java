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
import org.hibernate.search.engine.search.predicate.spi.PrefixPredicateBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ElasticsearchTextPrefixPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final JsonObjectAccessor PREFIX_ACCESSOR =
			JsonAccessor.root().property( "prefix" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<JsonNode> VALUE_ACCESSOR = JsonAccessor.root().property( "value" );

	private final JsonNode prefix;

	private ElasticsearchTextPrefixPredicate(Builder builder) {
		super( builder );
		this.prefix = builder.prefix;
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context, ObjectNode outerObject, ObjectNode innerObject) {
		VALUE_ACCESSOR.set( innerObject, prefix );
		ObjectNode middleObject = new ObjectNode( JsonNodeFactory.instance );
		middleObject.set( absoluteFieldPath, innerObject );
		PREFIX_ACCESSOR.set( outerObject, middleObject );
		return outerObject;
	}

	public static class Factory
			extends AbstractElasticsearchValueFieldSearchQueryElementFactory<PrefixPredicateBuilder, String> {

		@Override
		public PrefixPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<String> field) {
			return new Builder( scope, field );
		}
	}

	private static class Builder extends AbstractBuilder implements PrefixPredicateBuilder {

		private JsonNode prefix;

		private Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<String> field) {
			super( scope, field );
		}

		@Override
		public void prefix(String prefix) {
			this.prefix = JsonScalarConversions.toNode( prefix );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchTextPrefixPredicate( this );
		}
	}
}
