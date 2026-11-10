package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonCodecs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Determines whether two {@link JsonNode}s should be considered equivalent.
 *
 */
class JsonElementEquivalence {

	private final JsonElementEquivalence nestedEquivalence;

	JsonElementEquivalence() {
		this.nestedEquivalence = this; // Use the same equivalence for array items and object properties
	}

	JsonElementEquivalence(JsonElementEquivalence itemEquivalence) {
		this.nestedEquivalence = itemEquivalence; // Use the given equivalence for array items and object properties
	}

	/**
	 * Determines whether two {@link JsonNode}s should be considered equivalent.
	 * @param left An element whose equivalence to {@code right} will be tested.
	 * @param right An element whose equivalence to {@code left} will be tested.
	 * @return {@code true} if {@code left} and {@code right} are equivalent, {@code false} otherwise.
	 */
	public boolean isEquivalent(JsonNode left, JsonNode right) {
		if ( left == null || right == null ) {
			return left == right;
		}
		else {
			if ( isPrimitive( left ) && isPrimitive( right ) ) {
				return isPrimitiveEquivalent( left, right );
			}
			else if ( left.isArray() && right.isArray() ) {
				return isArrayEquivalent( (ArrayNode) left, (ArrayNode) right );
			}
			else if ( left.isObject() && right.isObject() ) {
				return isObjectEquivalent( (ObjectNode) left, (ObjectNode) right );
			}
			else {
				return isElementEquivalent( left, right );
			}
		}
	}

	private static boolean isPrimitive(JsonNode node) {
		return node.isString() || node.isBoolean() || node.isNumber();
	}

	/*
	 * Compares the string representation of primitives.
	 *
	 * This is necessary when validating analysis settings because,
	 * when we retrieve settings from Elasticsearch,
	 * it only returns strings.
	 * The reason is probably that the values are stored as strings.
	 * Thus we must also handle the case where we initially set an integer value but Elasticsearch
	 * shows it as a string.
	 */
	protected boolean isPrimitiveEquivalent(JsonNode left, JsonNode right) {
		// Elasticsearch settings return strings, while number-node spelling may normalize.
		if ( ( left.isNumber() || left.isString() )
				&& ( right.isNumber() || right.isString() )
				&& ( left.isNumber() || right.isNumber() ) ) {
			try {
				return new BigDecimal( JsonCodecs.STRING.decode( left ) )
						.compareTo( new BigDecimal( JsonCodecs.STRING.decode( right ) ) ) == 0;
			}
			catch (NumberFormatException e) {
				return false;
			}
		}
		return Objects.equals( JsonCodecs.STRING.decode( left ), JsonCodecs.STRING.decode( right ) );
	}

	protected boolean isArrayEquivalent(ArrayNode left, ArrayNode right) {
		if ( left == null || right == null ) {
			return left == right;
		}

		if ( left.size() != right.size() ) {
			return false;
		}
		int size = left.size();
		for ( int i = 0; i < size; ++i ) {
			if ( !isNestedEquivalent( left.get( i ), right.get( i ) ) ) {
				return false;
			}
		}

		return true;
	}

	protected boolean isObjectEquivalent(ObjectNode left, ObjectNode right) {
		for ( Map.Entry<String, JsonNode> leftEntry : left.properties() ) {
			String propertyName = leftEntry.getKey();
			JsonNode leftValue = leftEntry.getValue();
			JsonNode rightValue = right.get( propertyName );
			if ( !isNestedEquivalent( leftValue, rightValue ) ) {
				return false;
			}
		}

		// Also check for properties that are only in "right"
		for ( Map.Entry<String, JsonNode> rightEntry : right.properties() ) {
			String propertyName = rightEntry.getKey();
			if ( !left.has( propertyName ) ) {
				JsonNode rightValue = rightEntry.getValue();
				// Let the equivalence decide whether null can be equivalent to something
				if ( !isNestedEquivalent( null, rightValue ) ) {
					return false;
				}
			}
		}

		return true;
	}

	protected boolean isNestedEquivalent(JsonNode left, JsonNode right) {
		return nestedEquivalence.isEquivalent( left, right );
	}

	/*
	 * Compare two elements that either aren't of the same type or are both NullNode.
	 */
	protected boolean isElementEquivalent(JsonNode left, JsonNode right) {
		return Objects.equals( left, right );
	}
}
