/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.predicate;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;

/**
 * A DSL context allowing to specify the type of a predicate, with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchPredicateContainerContext extends SearchPredicateContainerContext {

	SearchPredicateTerminalContext fromJsonString(String jsonString);

}
