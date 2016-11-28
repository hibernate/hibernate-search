/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class JsonElementType<T extends JsonElement> {

	public static final JsonElementType<JsonObject> OBJECT = new JsonElementType<JsonObject>() {
		@Override
		public JsonObject newInstance() {
			return new JsonObject();
		}

		@Override
		public JsonObject cast(JsonElement element) {
			return element == null || element.isJsonNull() ? null : element.getAsJsonObject();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element != null && element.isJsonObject();
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
			return element == null || element.isJsonNull() ? null : element.getAsJsonArray();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element != null && element.isJsonArray();
		}

		@Override
		public String toString() {
			return JsonArray.class.getSimpleName();
		}
	};

	public static final JsonElementType<JsonPrimitive> PRIMITIVE = new JsonElementType<JsonPrimitive>() {
		@Override
		public JsonPrimitive newInstance() {
			throw new UnsupportedOperationException();
		}

		@Override
		public JsonPrimitive cast(JsonElement element) {
			return element == null || element.isJsonNull() ? null : element.getAsJsonPrimitive();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element != null && element.isJsonPrimitive();
		}

		@Override
		public String toString() {
			return JsonPrimitive.class.getSimpleName();
		}
	};

	public static final JsonElementType<JsonNull> NULL = new JsonElementType<JsonNull>() {
		@Override
		public JsonNull newInstance() {
			return JsonNull.INSTANCE;
		}

		@Override
		public JsonNull cast(JsonElement element) {
			return element == null ? null : element.getAsJsonNull();
		}

		@Override
		public boolean isInstance(JsonElement element) {
			return element != null && element.isJsonNull();
		}

		@Override
		public String toString() {
			return JsonNull.class.getSimpleName();
		}
	};

	private JsonElementType() {
		// Not allowed
	}

	public abstract T newInstance();

	public abstract T cast(JsonElement element);

	public abstract boolean isInstance(JsonElement element);
}