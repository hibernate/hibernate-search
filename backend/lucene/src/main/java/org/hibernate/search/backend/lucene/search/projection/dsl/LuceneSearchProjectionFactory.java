/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.dsl;

import org.hibernate.search.engine.search.projection.dsl.ExtendedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

/**
 * A factory for search projections with some Lucene-specific methods.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 * @see SearchProjectionFactory
 */
public interface LuceneSearchProjectionFactory<R, E>
		extends ExtendedSearchProjectionFactory<LuceneSearchProjectionFactory<R, E>, R, E> {

	/**
	 * Project to a Lucene {@link Document} containing all the stored fields.
	 * <p>
	 * Note that only stored fields are returned: fields that are not marked as
	 * {@link org.hibernate.search.engine.backend.types.Projectable#YES projectable}
	 * may be missing.
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<Document> document();

	/**
	 * Project to a Lucene {@link Explanation} describing the score computation for the hit.
	 * <p>
	 * This feature is relatively expensive, do not use unless you return a limited
	 * amount of objects (using pagination).
	 *
	 * @return The final step of the projection DSL.
	 */
	ProjectionFinalStep<Explanation> explanation();

	/**
	 * Project to a {@link DocumentTree} containing all the stored fields and nested documents.
	 * <p>
	 * Note that only stored fields are returned: fields that are not marked as
	 * {@link org.hibernate.search.engine.backend.types.Projectable#YES projectable}
	 * may be missing.
	 *
	 * @return The final step of the projection DSL.
	 */
	@Incubating
	ProjectionFinalStep<DocumentTree> documentTree();

}
