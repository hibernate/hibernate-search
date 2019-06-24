/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.projection;

import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

/**
 * A DSL context allowing to create a projection, with some Lucene-specific methods.
 *
 * @param <R> The type of entity references.
 * @param <E> The type of entities.
 * @see SearchProjectionFactoryContext
 */
public interface LuceneSearchProjectionFactoryContext<R, E> extends SearchProjectionFactoryContext<R, E> {

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

}
