package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.common.impl.DocumentIdHelper;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generate the JSON for queries by id for Elasticsearch.
 * <p>
 * Example:
 * <pre>
 * {@code
 * GET /_search
 * {
 *     "query": {
 *         "ids" : {
 *             "values" : ["1", "4", "100"]
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * @author Davide D'Alto
 */
public class ElasticsearchMatchIdPredicate extends AbstractElasticsearchPredicate {

	private static final JsonObjectAccessor IDS_ACCESSOR =
			JsonAccessor.root().property( "ids" ).unwrapSingletonArrays().asObject();
	private static final JsonAccessor<JsonNode> VALUES_ACCESSOR = JsonAccessor.root().property( "values" );

	private final DocumentIdHelper documentIdHelper;
	private final List<String> values;

	private ElasticsearchMatchIdPredicate(Builder builder) {
		super( builder );
		documentIdHelper = builder.scope.documentIdHelper();
		values = builder.values;
		// Ensure illegal attempts to mutate the predicate will fail
		builder.values = null;
	}

	@Override
	public void checkNestableWithin(PredicateNestingContext context) {
		// Nothing to do
	}

	@Override
	protected ObjectNode doToJsonQuery(PredicateRequestContext context,
			ObjectNode outerObject, ObjectNode innerObject) {
		ArrayNode array = toJsonArray( values, context.getTenantId() );

		VALUES_ACCESSOR.set( innerObject, array );

		IDS_ACCESSOR.set( outerObject, innerObject );
		return outerObject;
	}

	private ArrayNode toJsonArray(List<String> list, String tenantId) {
		ArrayNode jsonArray = JsonNodeFactory.instance.arrayNode( list.size() );
		for ( String value : list ) {
			jsonArray.add( documentIdHelper.toElasticsearchId( tenantId, value ) );
		}
		return jsonArray;
	}

	static class Builder extends AbstractElasticsearchPredicate.AbstractBuilder implements MatchIdPredicateBuilder {

		private List<String> values = new ArrayList<>();

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void value(Object value, ValueModel valueModel) {
			DslConverter<?, String> converter = scope.identifier().dslConverter( valueModel );
			ToDocumentValueConvertContext context = scope.toDocumentValueConvertContext();
			values.add( converter.unknownTypeToDocumentValue( value, context ) );
		}

		@Override
		public SearchPredicate build() {
			return new ElasticsearchMatchIdPredicate( this );
		}
	}
}
