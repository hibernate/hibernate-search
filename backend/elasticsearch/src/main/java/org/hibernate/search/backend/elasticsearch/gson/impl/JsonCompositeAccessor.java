/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A {@link JsonAccessor} that can be crawled by a {@link AbstractCrawlingJsonAccessor}.
 *
 */
interface JsonCompositeAccessor<T extends JsonElement> extends JsonAccessor<T> {

	T getOrCreate(JsonObject root);

}
