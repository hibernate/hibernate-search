package org.hibernate.search.util.json.impl;

import java.util.ArrayDeque;

import org.hibernate.search.util.json.internal.spi.JsonNodes;
import org.hibernate.search.util.json.logging.impl.JsonLog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Stack-based tree construction; never uses accessor chains. */
final class JsonTreeBuilder {
	private final ArrayDeque<Frame> frames = new ArrayDeque<>();
	private JsonNode root;

	void add(JsonNode value) {
		if ( frames.isEmpty() ) {
			if ( root != null ) {
				throw JsonLog.INSTANCE.trailingContent();
			}
			root = value;
		}
		else {
			Frame frame = frames.peek();
			if ( frame.container.isArray() ) {
				( (ArrayNode) frame.container ).add( value );
			}
			else {
				if ( frame.name == null ) {
					throw JsonLog.INSTANCE.missingMemberName();
				}
				( (ObjectNode) frame.container ).set( frame.name, value );
				frame.name = null;
			}
		}
	}

	void startObject() {
		ObjectNode object = JsonNodes.objectNode();
		add( object );
		frames.push( new Frame( object ) );
	}

	void startArray() {
		ArrayNode array = JsonNodes.arrayNode();
		add( array );
		frames.push( new Frame( array ) );
	}

	void name(String name) {
		frames.peek().name = name;
	}

	void endContainer() {
		frames.pop();
	}

	JsonNode finish() {
		if ( root == null ) {
			throw JsonLog.INSTANCE.emptyInput();
		}
		if ( !frames.isEmpty() ) {
			throw JsonLog.INSTANCE.truncatedInput();
		}
		return root;
	}

	void clear() {
		root = null;
		frames.clear();
	}

	private static final class Frame {
		private final JsonNode container;
		private String name;

		private Frame(JsonNode container) {
			this.container = container;
		}
	}
}
