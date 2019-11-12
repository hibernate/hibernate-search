/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchPredicateBuilderFactory
		extends
		SearchPredicateBuilderFactory<ElasticsearchSearchPredicateCollector, ElasticsearchSearchPredicateBuilder> {

	ElasticsearchSearchPredicateBuilder fromJson(JsonObject jsonObject);

	ElasticsearchSearchPredicateBuilder fromJson(String jsonString);

}
