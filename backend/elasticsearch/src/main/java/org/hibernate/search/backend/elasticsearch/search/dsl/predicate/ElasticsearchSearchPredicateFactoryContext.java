/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate;

import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;

/**
 * A DSL context allowing to specify the type of a predicate, with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchPredicateFactoryContext extends SearchPredicateFactoryContext {

	/**
	 * Create a predicate from JSON.
	 *
	 * @param jsonString A string representing an Elasticsearch query as a JSON object.
	 * The JSON object must be a syntactically correct Elasticsearch query.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">the Elasticsearch documentation</a>.
	 * @return The final step of the predicate DSL.
	 */
	PredicateFinalStep fromJson(String jsonString);

}
