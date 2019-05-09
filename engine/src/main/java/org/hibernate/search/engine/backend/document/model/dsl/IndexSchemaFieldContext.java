/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * @param <S> The exposed type for this context.
 * @param <R> The reference type.
 */
public interface IndexSchemaFieldContext<S extends IndexSchemaFieldContext<?, R>, R> extends IndexSchemaFieldTerminalContext<R> {

	/**
	 * Mark the field as multi-valued.
	 * <p>
	 * This informs the backend that this field may contain multiple values for a single parent document or object.
	 * @return {@code this}, for method chaining.
	 */
	S multiValued();

}
