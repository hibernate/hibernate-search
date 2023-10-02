/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface JsonArrayAccessor extends JsonAccessor<JsonArray> {
	UnknownTypeJsonAccessor element(int index);

	/**
	 * Add the given JsonElement to the array this accessor points to for the given {@code root},
	 * unless it is already present.
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to add.
	 * @throws UnexpectedJsonElementTypeException If an element in the path has unexpected type, preventing
	 * write access to the array this accessor points to.
	 */
	void addElementIfAbsent(JsonObject root, JsonElement newValue);
}
