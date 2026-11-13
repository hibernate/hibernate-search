package org.hibernate.search.backend.elasticsearch.validation.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

/**
 * An {@link JsonElementEquivalence} that considers that arrays are unordered containers.
 *
 */
class JsonElementUnorderedArrayEquivalence extends JsonElementEquivalence {

	JsonElementUnorderedArrayEquivalence(JsonElementEquivalence nestedEquivalence) {
		super( nestedEquivalence );
	}

	@Override
	protected boolean isArrayEquivalent(ArrayNode left, ArrayNode right) {
		return containsAll( left, right ) && containsAll( right, left );
	}

	private boolean containsAll(ArrayNode containerToTest, ArrayNode elementsToFind) {
		for ( JsonNode elementToFind : elementsToFind ) {
			boolean found = false;
			for ( JsonNode candidate : containerToTest ) {
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
