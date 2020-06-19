/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;

import com.google.gson.JsonElement;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;

/**
 * A sort collector for Elasticsearch, using JSON to represent sorts.
 * <p>
 * Used by Elasticsearch-specific sort contributors.
 *
 * @see SearchSortBuilderFactory#contribute(Object, org.hibernate.search.engine.search.sort.SearchSort)
 * @see ElasticsearchSearchSortBuilder
 */
public interface ElasticsearchSearchSortCollector {

	PredicateRequestContext getRootPredicateContext();

	void collectSort(JsonElement sort);

	void collectDistanceSort(JsonElement sort, String absoluteFieldPath, GeoPoint center);

}
