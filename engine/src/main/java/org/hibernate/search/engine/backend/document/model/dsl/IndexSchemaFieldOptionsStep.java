/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * The final step in the definition of a field in the index schema,
 * where a reference to the field can be retrieved,
 * optionally setting some parameters beforehand.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <R> The reference type.
 *
 * @see IndexSchemaFieldFinalStep
 */
public interface IndexSchemaFieldOptionsStep<S extends IndexSchemaFieldOptionsStep<?, R>, R>
		extends IndexSchemaFieldFinalStep<R> {

	/**
	 * Mark the field as multi-valued.
	 * <p>
	 * This informs the backend that this field may contain multiple values for a single parent document or object.
	 * @return {@code this}, for method chaining.
	 */
	S multiValued();

}
