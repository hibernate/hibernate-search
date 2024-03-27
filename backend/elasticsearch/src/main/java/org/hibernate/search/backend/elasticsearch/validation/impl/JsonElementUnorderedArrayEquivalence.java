/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * An {@link JsonElementEquivalence} that considers that arrays are unordered containers.
 *
 */
class JsonElementUnorderedArrayEquivalence extends JsonElementEquivalence {

	JsonElementUnorderedArrayEquivalence(JsonElementEquivalence nestedEquivalence) {
		super( nestedEquivalence );
	}

	@Override
	protected boolean isArrayEquivalent(JsonArray left, JsonArray right) {
		return containsAll( left, right ) && containsAll( right, left );
	}

	private boolean containsAll(JsonArray containerToTest, JsonArray elementsToFind) {
		for ( JsonElement elementToFind : elementsToFind ) {
			boolean found = false;
			for ( JsonElement candidate : containerToTest ) {
				if ( isNestedEquivalent( elementToFind, candidate ) ) {
					found = true;
					break;
				}
			}
			if ( !found ) {
				return false;
			}
		}
		return true;
	}
}
