package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.spatial.GeoPoint;

import tools.jackson.databind.JsonNode;

/**
 * A sort collector for Elasticsearch, using JSON to represent sorts.
 * <p>
 * Used by Elasticsearch-specific sort contributors.
 *
 * @see ElasticsearchSearchSort#toJsonSorts(ElasticsearchSearchSortCollector)
 */
public interface ElasticsearchSearchSortCollector {

	PredicateRequestContext getRootPredicateContext();

	void collectSort(JsonNode sort);

	void collectDistanceSort(JsonNode sort, String absoluteFieldPath, GeoPoint center);

}
