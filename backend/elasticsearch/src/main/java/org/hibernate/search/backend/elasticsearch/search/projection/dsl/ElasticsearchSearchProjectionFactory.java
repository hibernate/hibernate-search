package org.hibernate.search.backend.elasticsearch.search.projection.dsl;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.engine.search.projection.dsl.ExtendedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

/**
 * A factory for search projections with some Elasticsearch-specific methods.
 *
 * @param <SR> Scope root type.
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 * @see TypedSearchProjectionFactory
 */
public interface ElasticsearchSearchProjectionFactory<SR, R, E>
		extends ExtendedSearchProjectionFactory<SR, ElasticsearchSearchProjectionFactory<SR, R, E>, R, E> {

	/**
	 * Project to a {@link JsonObject} representing the document as stored in Elasticsearch.
	 *
	 * @return The final step of the projection DSL.
	 */
	@Deprecated
	default ProjectionFinalStep<JsonObject> source() {
		return source( GsonJsonHandlers.object() );
	}

	<T> ProjectionFinalStep<T> source(JsonHandler<T> handler);

	/**
	 * Project to a {@link JsonObject} describing the score computation for the hit.
	 * <p>
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of hits (using pagination).
	 *
	 * @return The final step of the projection DSL.
	 */
	@Deprecated
	default ProjectionFinalStep<JsonObject> explanation() {
		return explanation( GsonJsonHandlers.object() );
	}

	<T> ProjectionFinalStep<T> explanation(JsonHandler<T> handler);

	/**
	 * Project to a {@link JsonObject} representing the hit as returned by Elasticsearch.
	 *
	 * @return The final step of the projection DSL.
	 */
	@Deprecated
	default ProjectionFinalStep<JsonObject> jsonHit() {
		return jsonHit( GsonJsonHandlers.object() );
	}

	<T> ProjectionFinalStep<T> jsonHit(JsonHandler<T> handler);

}
