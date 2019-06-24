/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.projection;

import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;

/**
 * A factory for search projections with some Elasticsearch-specific methods.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 * @see SearchProjectionFactory
 */
public interface ElasticsearchSearchProjectionFactory<R, E> extends SearchProjectionFactory<R, E> {

	/**
	 * Project to a string representing the JSON document as stored in Elasticsearch.
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<String> source();

	/**
	 * Project to a string representing a JSON object describing the score computation for the hit.
	 * <p>
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination).
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<String> explanation();

}
