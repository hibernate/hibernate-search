/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;

public abstract class JsonElementType<T> {

	JsonElementType() {
		// Not allowed
	}

	public final T fromElement(JsonElement element) {
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

	protected abstract T nullUnsafeFromElement(JsonElement element);

	public final JsonElement toElement(T value) {
		if ( value == null ) {
			return null;
		}
		else {
			return nullUnsafeToElement( value );
		}
	}

	protected abstract JsonElement nullUnsafeToElement(T element);

	public final boolean isInstance(JsonElement element) {
		return element != null && nullUnsafeIsInstance( element );
	}

	protected abstract boolean nullUnsafeIsInstance(JsonElement element);
}
