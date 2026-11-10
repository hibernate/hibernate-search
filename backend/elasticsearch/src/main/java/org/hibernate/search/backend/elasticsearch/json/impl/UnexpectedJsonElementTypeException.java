package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.JsonNode;

public class UnexpectedJsonElementTypeException extends AssertionFailure {

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, JsonElementType<?> expectedType,
			JsonNode actualElement) {
		this( String.valueOf( accessor ), expectedType, actualElement );
	}

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, List<? extends JsonElementType<?>> expectedTypes,
			JsonNode actualElement) {
		this( String.valueOf( accessor ), expectedTypes, actualElement );
	}

	public UnexpectedJsonElementTypeException(String path, JsonElementType<?> expectedType, JsonNode actualElement) {
		this( path, Collections.singletonList( expectedType ), actualElement );
	}

	public UnexpectedJsonElementTypeException(String path, List<? extends JsonElementType<?>> expectedTypes,
			JsonNode actualElement) {
		super( "Unexpected type at '" + path + "'. Expected one of " + expectedTypes + ", got '" + actualElement + "'" );
	}

}
