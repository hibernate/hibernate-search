package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class RootJsonAccessor implements JsonCompositeAccessor<JsonNode> {

	// Wrap the instance in a JsonObjectAccessor, because we know the root is an object
	public static final JsonObjectAccessor INSTANCE = new JsonObjectAccessorImpl( new RootJsonAccessor() );

	private RootJsonAccessor() {
		// Private, use INSTANCE instead.
	}

	@Override
	public Optional<JsonNode> get(ObjectNode root) {
		return Optional.of( requireRoot( root ) );
	}

	private ObjectNode requireRoot(ObjectNode root) {
		if ( root == null ) {
			throw new AssertionFailure( "A null root was encountered" );
		}
		else {
			return root;
		}
	}

	@Override
	public void set(ObjectNode root, JsonNode value) {
		throw new UnsupportedOperationException( "Cannot set the root element" );
	}

	@Override
	public ObjectNode getOrCreate(ObjectNode root, Supplier<? extends JsonNode> newValueSupplier) {
		return requireRoot( root );
	}

	@Override
	public ObjectNode getOrCreate(ObjectNode root) {
		return requireRoot( root );
	}

	@Override
	public String toString() {
		return "root";
	}
}
