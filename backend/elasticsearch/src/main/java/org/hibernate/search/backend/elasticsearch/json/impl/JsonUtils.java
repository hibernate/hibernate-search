package org.hibernate.search.backend.elasticsearch.json.impl;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.util.json.internal.spi.JsonNodes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class JsonUtils {

	private JsonUtils() {
	}

	/**
	 * Sets the given {@code newValue} as the value of property {@code propertyName} on {@code parent},
	 * wrapping it in an array if necessary.
	 *
	 * <p>The behavior is as follows:
	 * <ul>
	 * <li>If there is currently no value for this property, the property is simply set to {@code newValue}
	 * using {@link ObjectNode#set(String, JsonNode)}.
	 * <li>If the current value of the property is an array, {@code newValue} is added to this array.
	 * <li>Otherwise, the current value
	 * is replaced by an array containing the current value followed by the {@code newValue}.
	 * </ul>
	 *
	 * @param object The object whose property must be set.
	 * @param propertyName The name of the property to set.
	 * @param newValue The value to set.
	 */
	public static void setOrAppendToArray(ObjectNode object, String propertyName, JsonNode newValue) {
		JsonNode currentValue = object.get( propertyName );
		if ( currentValue == null ) {
			// Do not overwrite JSON null, because it might be there on purpose
			object.set( propertyName, newValue );
		}
		else if ( JsonElementTypes.ARRAY.isInstance( currentValue ) ) {
			JsonElementTypes.ARRAY.fromElement( currentValue ).add( newValue );
		}
		else {
			ArrayNode array = JsonNodes.arrayNode();
			array.add( currentValue );
			array.add( newValue );
			object.set( propertyName, array );
		}
	}

	/**
	 * Performs a structural copy through an explicitly registered tree codec, without rendering or parsing text.
	 * @param json The {@link JsonCodecService} object.
	 * @param objectType The type of the object to copy.
	 * @param object The object to copy.
	 * @param <T> The type of the object to copy.
	 * @return A deep copy of {@code object}.
	 */
	public static <T> T deepCopy(JsonCodecService json, Class<T> objectType, T object) {
		return json.decode( json.encode( object ), objectType );
	}
}
