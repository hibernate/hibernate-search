/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * A {@link JsonAccessor} that ensures the accessed object is a {@link JsonObject}.
 *
 * @author Yoann Rodiere
 */
public class JsonObjectAccessorImpl extends TypingJsonAccessor<JsonObject>
		implements JsonObjectAccessor, JsonCompositeAccessor<JsonObject> {

	public JsonObjectAccessorImpl(JsonAccessor<JsonElement> parent) {
		super( parent );
	}

	@Override
	protected JsonElementType<JsonObject> getExpectedElementType() {
		return JsonElementType.OBJECT;
	}

	@Override
	public JsonObject getOrCreate(JsonObject root) throws UnexpectedJsonElementTypeException {
		return getOrCreate( root, JsonObject::new );
	}

	@Override
	public UnknownTypeJsonAccessor property(String propertyName) {
		return new ObjectPropertyJsonAccessor( this, propertyName );
	}

}
