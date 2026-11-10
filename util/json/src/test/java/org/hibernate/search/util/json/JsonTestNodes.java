package org.hibernate.search.util.json;

import org.hibernate.search.util.json.internal.spi.JsonNumbers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

/**
 * Test-only construction without a replacement tree or automatic Java mapping.
 */
public final class JsonTestNodes {

	private JsonTestNodes() {
	}

	public static JsonNode scalar(Object value) {
		if ( value == null ) {
			return NullNode.instance;
		}
		if ( value instanceof String text ) {
			return StringNode.valueOf( text );
		}
		if ( value instanceof Boolean bool ) {
			return BooleanNode.valueOf( bool );
		}
		if ( value instanceof Number number ) {
			return JsonNumbers.fromNumber( number );
		}
		throw new IllegalArgumentException( "Unsupported test scalar" );
	}
}
