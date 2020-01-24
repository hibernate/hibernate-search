/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.util.regex.Pattern;

import org.hibernate.search.util.common.impl.Contracts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * A {@link JsonAccessor} that ensures the accessed object is a {@link JsonObject}.
 *
 */
public class JsonObjectAccessorImpl extends AbstractTypingJsonAccessor<JsonObject>
		implements JsonObjectAccessor, JsonCompositeAccessor<JsonObject> {

	private static final Pattern DOT_REGEX = Pattern.compile( "\\." );

	public JsonObjectAccessorImpl(JsonAccessor<JsonElement> parent) {
		super( parent );
	}

	@Override
	protected JsonElementType<JsonObject> getExpectedElementType() {
		return JsonElementTypes.OBJECT;
	}

	@Override
	public JsonObject getOrCreate(JsonObject root) {
		return getOrCreate( root, JsonObject::new );
	}

	@Override
	public UnknownTypeJsonAccessor property(String propertyName) {
		Contracts.assertNotNullNorEmpty( propertyName, "propertyName" );
		return new ObjectPropertyJsonAccessor( this, propertyName );
	}

	@Override
	public UnknownTypeJsonAccessor path(String dotSeparatedPath) {
		Contracts.assertNotNullNorEmpty( dotSeparatedPath, "dotSeparatedPath" );

		String[] components = DOT_REGEX.split( dotSeparatedPath );
		String leaf = components[components.length - 1];

		JsonObjectAccessor parent = this;

		for ( int i = 0; i < components.length - 1; i++ ) {
			parent = parent.property( components[i] ).asObject();
		}

		return parent.property( leaf );
	}
}
