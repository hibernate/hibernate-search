/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

import com.google.gson.JsonObject;


/**
 * A predicate collector for Elasticsearch, using JSON to represent predicates.
 * <p>
 * Used by Elasticsearch-specific predicate contributors.
 *
 * @see ElasticsearchSearchPredicateBuilderFactoryImpl#contribute(ElasticsearchSearchPredicateCollector, SearchPredicate)
 * @see ElasticsearchSearchPredicateBuilder
 */
public interface ElasticsearchSearchPredicateCollector {

	PredicateRequestContext getRootPredicateContext();

	void collectPredicate(JsonObject jsonQuery);

}
