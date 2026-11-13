package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonNodes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


/**
 * A {@link JsonAccessor} that ensures the accessed object is a {@link ObjectNode}.
 *
 */
public class JsonObjectAccessorImpl extends AbstractTypingJsonAccessor<ObjectNode>
		implements JsonObjectAccessor, JsonCompositeAccessor<ObjectNode> {

	public JsonObjectAccessorImpl(JsonAccessor<JsonNode> parent) {
		super( parent );
	}

	@Override
	protected JsonElementType<ObjectNode> getExpectedElementType() {
		return JsonElementTypes.OBJECT;
	}

	@Override
	public ObjectNode getOrCreate(ObjectNode root) {
		return getOrCreateNode( root, JsonNodes::objectNode );
	}

	@Override
	public UnknownTypeJsonAccessor property(String propertyName) {
		Objects.requireNonNull( propertyName );
		return new ObjectPropertyJsonAccessor( this, propertyName );
	}
}
