/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public interface ElasticsearchSearchSyntax {

	String getTermAggregationOrderByTermToken();

	void requestDocValues(JsonObject requestBody, JsonPrimitive fieldName);

	void requestNestedSort(List<String> nestedPathHierarchy, JsonObject innerObject, JsonObject filterOrNull);

	void requestGeoDistanceSortIgnoreUnmapped(JsonObject innerObject);

	JsonElement encodeLongForAggregation(Long value);

}
