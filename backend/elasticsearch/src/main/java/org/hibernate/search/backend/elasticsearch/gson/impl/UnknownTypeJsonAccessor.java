/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;

public interface UnknownTypeJsonAccessor extends JsonAccessor<JsonElement> {

	JsonObjectAccessor asObject();

	JsonArrayAccessor asArray();

	JsonAccessor<String> asString();

	JsonAccessor<Boolean> asBoolean();

	JsonAccessor<Integer> asInteger();

	JsonAccessor<Long> asLong();

	JsonAccessor<Float> asFloat();

	JsonAccessor<Double> asDouble();

	UnknownTypeJsonAccessor element(int index);

	UnknownTypeJsonAccessor property(String propertyName);

}
