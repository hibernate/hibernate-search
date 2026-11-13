package org.hibernate.search.backend.elasticsearch.lowlevel.syntax.search.impl;

import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public interface ElasticsearchSearchSyntax {

	String getTermAggregationOrderByTermToken();

	void requestDocValues(ObjectNode requestBody, JsonNode fieldName);

	void requestNestedSort(List<String> nestedPathHierarchy, ObjectNode innerObject, ObjectNode filterOrNull);

	void requestGeoDistanceSortIgnoreUnmapped(ObjectNode innerObject);

	JsonNode encodeLongForAggregation(Long value);
}
