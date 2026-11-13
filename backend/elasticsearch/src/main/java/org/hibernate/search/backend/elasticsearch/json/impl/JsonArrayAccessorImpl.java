package org.hibernate.search.backend.elasticsearch.json.impl;

import org.hibernate.search.util.json.internal.spi.JsonNodes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonArrayAccessorImpl extends AbstractTypingJsonAccessor<ArrayNode>
		implements JsonArrayAccessor, JsonCompositeAccessor<ArrayNode> {

	public JsonArrayAccessorImpl(JsonAccessor<JsonNode> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<ArrayNode> getExpectedElementType() {
		return JsonElementTypes.ARRAY;
	}

	@Override
	public ArrayNode getOrCreate(ObjectNode root) {
		return getOrCreateNode( root, JsonNodes::arrayNode );
	}

	@Override
	public UnknownTypeJsonAccessor element(int index) {
		return new ArrayElementJsonAccessor( this, index );
	}

	private static boolean contains(ArrayNode array, JsonNode value) {
		for ( JsonNode item : array ) {
			if ( JsonScalarConversions.equivalent( item, value ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void addElementIfAbsent(ObjectNode root, JsonNode newValue) {
		ArrayNode array = getOrCreate( root );
		if ( array == null ) {
			throw new IllegalStateException( "Cannot modify an absent adapted array at " + this );
		}
		if ( !contains( array, newValue ) ) {
			array.add( newValue );
		}
	}
}
