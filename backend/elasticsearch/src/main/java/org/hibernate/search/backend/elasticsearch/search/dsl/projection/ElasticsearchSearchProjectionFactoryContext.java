/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.projection;

import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;

/**
 * A DSL context allowing to create a projection, with some Elasticsearch-specific methods.
 *
 * @param <R> The type of references.
 * @param <O> The type of entities.
 * @see SearchProjectionFactoryContext
 */
public interface ElasticsearchSearchProjectionFactoryContext<R, O> extends SearchProjectionFactoryContext<R, O> {

	/**
	 * Project to a string representing the JSON document as stored in Elasticsearch.
	 * @return A context allowing to define the projection more precisely.
	 */
	SearchProjectionTerminalContext<String> source();

	/**
	 * Project to a string representing a JSON object describing the score computation for the hit.
	 * <p>
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination).
	 *
	 * @return A context allowing to define the projection more precisely.
	 */
	SearchProjectionTerminalContext<String> explanation();

}
