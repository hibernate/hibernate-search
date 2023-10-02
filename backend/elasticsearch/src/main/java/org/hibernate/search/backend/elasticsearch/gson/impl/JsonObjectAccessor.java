/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonObject;

public interface JsonObjectAccessor extends JsonAccessor<JsonObject> {

	UnknownTypeJsonAccessor property(String propertyName);

}
