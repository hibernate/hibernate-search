/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.dsl;

import org.hibernate.search.engine.search.projection.dsl.ExtendedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;

import com.google.gson.JsonObject;

/**
 * A factory for search projections with some Elasticsearch-specific methods.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 * @see SearchProjectionFactory
 */
public interface ElasticsearchSearchProjectionFactory<R, E>
		extends ExtendedSearchProjectionFactory<ElasticsearchSearchProjectionFactory<R, E>, R, E> {

	/**
	 * Project to a {@link JsonObject} representing the document as stored in Elasticsearch.
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<JsonObject> source();

	/**
	 * Project to a {@link JsonObject} describing the score computation for the hit.
	 * <p>
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of hits (using pagination).
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<JsonObject> explanation();

	/**
	 * Project to a {@link JsonObject} representing the hit as returned by Elasticsearch.
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<JsonObject> jsonHit();

}
