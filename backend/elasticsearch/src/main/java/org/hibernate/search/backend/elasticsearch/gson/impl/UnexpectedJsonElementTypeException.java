/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.JsonElement;

public class UnexpectedJsonElementTypeException extends AssertionFailure {

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, JsonElementType<?> expectedType,
			JsonElement actualElement) {
		this( String.valueOf( accessor ), expectedType, actualElement );
	}

	public UnexpectedJsonElementTypeException(JsonAccessor<?> accessor, List<? extends JsonElementType<?>> expectedTypes,
			JsonElement actualElement) {
		this( String.valueOf( accessor ), expectedTypes, actualElement );
	}

	public UnexpectedJsonElementTypeException(String path, JsonElementType<?> expectedType, JsonElement actualElement) {
		this( path, Collections.singletonList( expectedType ), actualElement );
	}

	public UnexpectedJsonElementTypeException(String path, List<? extends JsonElementType<?>> expectedTypes,
			JsonElement actualElement) {
		super( "Unexpected type at '" + path + "'. Expected one of " + expectedTypes + ", got '" + actualElement + "'" );
	}

}
