/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
