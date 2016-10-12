/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

abstract class JsonElementType<T extends JsonElement> {

	public static final JsonElementType<JsonObject> OBJECT = new JsonElementType<JsonObject>() {
		@Override
		public JsonObject newInstance() {
			return new JsonObject();
		}

		@Override
		public JsonObject cast(JsonElement element) {
			return element.getAsJsonObject();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element.isJsonObject();
		}

		@Override
		public String toString() {
			return JsonObject.class.getSimpleName();
		}
	};

	public static final JsonElementType<JsonArray> ARRAY = new JsonElementType<JsonArray>() {
		@Override
		public JsonArray newInstance() {
			return new JsonArray();
		}

		@Override
		public JsonArray cast(JsonElement element) {
			return element.getAsJsonArray();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element.isJsonArray();
		}

		@Override
		public String toString() {
			return JsonArray.class.getSimpleName();
		}
	};

	private JsonElementType() {
		// Not allowed
	}

	public abstract T newInstance();

	public abstract T cast(JsonElement element);

	public abstract boolean isInstance(JsonElement element);
}