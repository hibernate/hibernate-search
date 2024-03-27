/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Determines whether two {@link JsonElement}s should be considered equivalent.
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
	 * Determines whether two {@link JsonElement}s should be considered equivalent.
	 * @param left An element whose equivalence to {@code right} will be tested.
	 * @param right An element whose equivalence to {@code left} will be tested.
	 * @return {@code true} if {@code left} and {@code right} are equivalent, {@code false} otherwise.
	 */
	public boolean isEquivalent(JsonElement left, JsonElement right) {
		if ( left == null || right == null ) {
			return left == right;
		}
		else {
			if ( left.isJsonPrimitive() && right.isJsonPrimitive() ) {
				return isPrimitiveEquivalent( left.getAsJsonPrimitive(), right.getAsJsonPrimitive() );
			}
			else if ( left.isJsonArray() && right.isJsonArray() ) {
				return isArrayEquivalent( left.getAsJsonArray(), right.getAsJsonArray() );
			}
			else if ( left.isJsonObject() && right.isJsonObject() ) {
				return isObjectEquivalent( left.getAsJsonObject(), right.getAsJsonObject() );
			}
			else {
				return isElementEquivalent( left, right );
			}
		}
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
	protected boolean isPrimitiveEquivalent(JsonPrimitive left, JsonPrimitive right) {
		return Objects.equals( left.getAsString(), right.getAsString() );
	}

	protected boolean isArrayEquivalent(JsonArray left, JsonArray right) {
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

	protected boolean isObjectEquivalent(JsonObject left, JsonObject right) {
		for ( Map.Entry<String, JsonElement> leftEntry : left.entrySet() ) {
			String propertyName = leftEntry.getKey();
			JsonElement leftValue = leftEntry.getValue();
			JsonElement rightValue = right.get( propertyName );
			if ( !isNestedEquivalent( leftValue, rightValue ) ) {
				return false;
			}
		}

		// Also check for properties that are only in "right"
		for ( Map.Entry<String, JsonElement> rightEntry : right.entrySet() ) {
			String propertyName = rightEntry.getKey();
			if ( !left.has( propertyName ) ) {
				JsonElement rightValue = rightEntry.getValue();
				// Let the equivalence decide whether null can be equivalent to something
				if ( !isNestedEquivalent( null, rightValue ) ) {
					return false;
				}
			}
		}

		return true;
	}

	protected boolean isNestedEquivalent(JsonElement left, JsonElement right) {
		return nestedEquivalence.isEquivalent( left, right );
	}

	/*
	 * Compare two elements that either aren't of the same type or are both JsonNull.
	 */
	protected boolean isElementEquivalent(JsonElement left, JsonElement right) {
		return Objects.equals( left, right );
	}
}
