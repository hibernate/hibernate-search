package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The search syntax for ES8.1 and later.
 */
public class Elasticsearch81SearchSyntax implements ElasticsearchSearchSyntax {

	private static final JsonArrayAccessor DOCVALUE_FIELDS_ACCESSOR =
			JsonAccessor.root().property( "docvalue_fields" ).asArray();

	private static final JsonAccessor<JsonNode> NESTED_ACCESSOR = JsonAccessor.root().property( "nested" );

	private static final JsonAccessor<JsonNode> PATH_ACCESSOR = JsonAccessor.root().property( "path" );

	private static final JsonAccessor<JsonNode> FILTER_ACCESSOR = JsonAccessor.root().property( "filter" );

	private static final JsonAccessor<Boolean> IGNORE_UNMAPPED_ACCESSOR =
			JsonAccessor.root().property( "ignore_unmapped" ).unwrapSingletonArrays().asBoolean();

	@Override
	public String getTermAggregationOrderByTermToken() {
		return "_key";
	}

	@Override
	public void requestDocValues(ObjectNode requestBody, JsonNode fieldName) {
		// The default format is the format defined in the mapping, which is what we want
		DOCVALUE_FIELDS_ACCESSOR.addElementIfAbsent( requestBody, fieldName );
	}

	@Override
	public void requestNestedSort(List<String> nestedPathHierarchy, ObjectNode innerObject, ObjectNode filterOrNull) {
		ObjectNode nextNestedObjectTarget = innerObject;
		for ( int i = 0; i < nestedPathHierarchy.size(); i++ ) {
			String nestedPath = nestedPathHierarchy.get( i );
			ObjectNode nestedObject = new ObjectNode( JsonNodeFactory.instance );
			PATH_ACCESSOR.set( nestedObject, JsonScalarConversions.toNode( nestedPath ) );
			NESTED_ACCESSOR.set( nextNestedObjectTarget, nestedObject );
			if ( i == ( nestedPathHierarchy.size() - 1 ) && filterOrNull != null ) {
				FILTER_ACCESSOR.set( nestedObject, filterOrNull );
			}
			// the new api requires a recursion on the path hierarchy
			nextNestedObjectTarget = nestedObject;
		}
	}

	@Override
	public void requestGeoDistanceSortIgnoreUnmapped(ObjectNode innerObject) {
		IGNORE_UNMAPPED_ACCESSOR.set( innerObject, true );
	}

	@Override
	public JsonNode encodeLongForAggregation(Long value) {
		// https://github.com/elastic/elasticsearch/issues/81529 was solved in ES8.1
		return value == null ? NullNode.instance : JsonScalarConversions.toNode( value );
	}
}
