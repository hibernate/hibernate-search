/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;

/**
 * A sort collector for Elasticsearch, using JSON to represent sorts.
 * <p>
 * Used by Elasticsearch-specific sort contributors.
 *
 * @see ElasticsearchSearchSort#toJsonSorts(ElasticsearchSearchSortCollector)
 */
public interface ElasticsearchSearchSortCollector {

	PredicateRequestContext getRootPredicateContext();

	void collectSort(JsonElement sort);

	void collectDistanceSort(JsonElement sort, String absoluteFieldPath, GeoPoint center);

}
