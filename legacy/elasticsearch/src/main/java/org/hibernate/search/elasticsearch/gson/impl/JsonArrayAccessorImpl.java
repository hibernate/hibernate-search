/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.gson.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class JsonArrayAccessorImpl extends TypingJsonAccessor<JsonArray>
		implements JsonArrayAccessor, JsonCompositeAccessor<JsonArray> {

	public JsonArrayAccessorImpl(JsonAccessor<JsonElement> parentAccessor) {
		super( parentAccessor );
	}

	@Override
	protected JsonElementType<JsonArray> getExpectedElementType() {
		return JsonElementType.ARRAY;
	}

	@Override
	public JsonArray getOrCreate(JsonObject root) throws UnexpectedJsonElementTypeException {
		return getOrCreate( root, JsonArray::new );
	}

	@Override
	public UnknownTypeJsonAccessor element(int index) {
		return new ArrayElementJsonAccessor( this, index );
	}
}
