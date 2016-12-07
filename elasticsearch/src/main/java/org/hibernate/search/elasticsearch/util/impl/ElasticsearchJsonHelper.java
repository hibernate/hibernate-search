/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import com.google.gson.JsonPrimitive;

/**
 * Various utilities to manipulate Json in Elasticsearch.
 *
 * @author Yoann Rodiere
 */
public final class ElasticsearchJsonHelper {

	private ElasticsearchJsonHelper() {
		// Not allowed
	}

	/**
	 * @param value An object to convert to a JsonPrimitive. Only {@code null}, {@code Boolean},
	 * {@code Number} and {@code String} are allowed.
	 * @return A JsonPrimitive representing {@code value}
	 * @throws IllegalArgumentException If the given value is an instance of an unsupported type.
	 */
	public static JsonPrimitive toJsonPrimitive(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String ) {
			return new JsonPrimitive( (String) value );
		}
		else if ( value instanceof Number ) {
			return new JsonPrimitive( (Number) value );
		}
		else if ( value instanceof Boolean ) {
			return new JsonPrimitive( (Boolean) value );
		}
		else {
			throw new IllegalArgumentException( "Value '" + value + "' has unexpected type" );
		}
	}

}
