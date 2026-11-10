package org.hibernate.search.backend.elasticsearch.json.impl;

import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.JsonNode;


public abstract class JsonElementType<T> {

	protected JsonElementType() {
		// Not allowed
	}

	public final T fromElement(JsonNode element) {
		if ( element == null ) {
			return null;
		}
		else if ( isInstance( element ) ) {
			return nullUnsafeFromElement( element );
		}
		else {
			/*
			 * Callers are supposed to call isInstance first,
			 * so failing here is actually an internal error.
			 */
			throw new AssertionFailure( element + " cannot be cast to " + this );
		}
	}

	protected abstract T nullUnsafeFromElement(JsonNode element);

	public final JsonNode toElement(T value) {
		if ( value == null ) {
			return null;
		}
		else {
			return nullUnsafeToElement( value );
		}
	}

	protected abstract JsonNode nullUnsafeToElement(T element);

	public final boolean isInstance(JsonNode element) {
		return element != null && nullUnsafeIsInstance( element );
	}

	protected abstract boolean nullUnsafeIsInstance(JsonNode element);
}
