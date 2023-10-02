/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class GsonUtils {

	private GsonUtils() {
	}

	/**
	 * Sets the given {@code newValue} as the value of property {@code propertyName} on {@code parent},
	 * wrapping it in an array if necessary.
	 *
	 * <p>The behavior is as follows:
	 * <ul>
	 * <li>If there is currently no value for this property, the property is simply set to {@code newValue}
	 * using {@link JsonObject#add(String, JsonElement)}.
	 * <li>If the current value of the property is an array, {@code newValue} is added to this array.
	 * <li>Otherwise, the current value
	 * is replaced by an array containing the current value followed by the {@code newValue}.
	 * </ul>
	 *
	 * @param object The object whose property must be set.
	 * @param propertyName The name of the property to set.
	 * @param newValue The value to set.
	 */
	public static void setOrAppendToArray(JsonObject object, String propertyName, JsonElement newValue) {
		JsonElement currentValue = object.get( propertyName );
		if ( currentValue == null ) { // Do not overwrite JsonNull, because it might be there on purpose
			object.add( propertyName, newValue );
		}
		else if ( JsonElementTypes.ARRAY.isInstance( currentValue ) ) {
			JsonElementTypes.ARRAY.fromElement( currentValue ).add( newValue );
		}
		else {
			JsonArray array = new JsonArray();
			array.add( currentValue );
			array.add( newValue );
			object.add( propertyName, array );
		}
	}

	/**
	 * Efficiently performs a deep copy of a given object using Gson serialization/deserialization.
	 * @param gson The {@link Gson} object.
	 * @param objectType The type of the object to copy.
	 * @param object The object to copy.
	 * @param <T> The type of the object to copy.
	 * @return A deep copy of {@code object}.
	 */
	public static <T> T deepCopy(Gson gson, Class<T> objectType, T object) {
		return gson.fromJson( gson.toJsonTree( object ), objectType );
	}

}
