package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Optional;
import java.util.function.Supplier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;


/** Read adaptation never changes a selected wrapper; set replaces the selected raw location. */
final class SingletonArrayJsonAccessor extends AbstractCrawlingJsonAccessor<JsonNode> {
	private final UnknownTypeJsonAccessor selected;

	SingletonArrayJsonAccessor(UnknownTypeJsonAccessor selected) {
		super( new SelectedLocation( selected ) );
		this.selected = selected;
	}

	@Override
	protected JsonNode doGet(JsonNode parent) {
		return unwrap( parent );
	}

	@Override
	protected void doSet(JsonNode parent, JsonNode value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(ObjectNode root, JsonNode value) {
		selected.set( root, value );
	}

	@Override
	public JsonNode getOrCreate(ObjectNode root, Supplier<? extends JsonNode> supplier) {
		return unwrap( selected.getOrCreate( root, supplier ) );
	}

	private JsonNode unwrap(JsonNode value) {
		while ( value != null && value.isArray() ) {
			ArrayNode array = (ArrayNode) value;
			if ( array.size() == 0 ) {
				return null;
			}
			if ( array.size() != 1 ) {
				throw new IllegalStateException(
						"Expected singleton array at " + this + ", got " + array.size() + " elements" );
			}
			value = array.get( 0 );
		}
		return value;
	}

	@Override
	protected void appendRuntimeRelativePath(StringBuilder path) {
		path.append( "(unwrapSingletonArrays)" );
	}

	@Override
	protected void appendStaticRelativePath(StringBuilder path, boolean first) {
	}

	private static final class SelectedLocation implements JsonCompositeAccessor<JsonNode> {
		private final JsonAccessor<JsonNode> selected;

		private SelectedLocation(JsonAccessor<JsonNode> selected) {
			this.selected = selected;
		}

		@Override
		public Optional<JsonNode> get(ObjectNode root) {
			return selected.get( root );
		}

		@Override
		public void set(ObjectNode root, JsonNode value) {
			selected.set( root, value );
		}

		@Override
		public JsonNode getOrCreate(ObjectNode root, Supplier<? extends JsonNode> supplier) {
			return selected.getOrCreate( root, supplier );
		}

		@Override
		public JsonNode getOrCreate(ObjectNode root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return selected.toString();
		}
	}
}
