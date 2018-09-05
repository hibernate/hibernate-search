/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;

/**
 * @author Yoann Rodiere
 */
public class UnexpectedJsonElementTypeException extends AssertionFailure {

	private final JsonAccessor<?> accessor;
	private final List<JsonElementType<?>> expectedTypes;
	private final JsonElement actualElement;

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, JsonElementType<?> expectedType, JsonElement actualElement) {
		this( accessor, Arrays.asList( expectedType ), actualElement );
	}

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, List<? extends JsonElementType<?>> expectedTypes, JsonElement actualElement) {
		super( "Unexpected type at '" + accessor + "'. Expected one of " + expectedTypes + ", got '" + actualElement + "'" );
		this.accessor = accessor;
		this.expectedTypes = Collections.unmodifiableList( new ArrayList<JsonElementType<?>>( expectedTypes ) );
		this.actualElement = actualElement;
	}

	public JsonAccessor<?> getAccessor() {
		return accessor;
	}

	public List<JsonElementType<?>> getExpectedTypes() {
		return expectedTypes;
	}

	public JsonElement getActualElement() {
		return actualElement;
	}

}
