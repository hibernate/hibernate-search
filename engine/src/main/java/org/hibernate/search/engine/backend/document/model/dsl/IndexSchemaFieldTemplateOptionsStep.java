/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl;

/**
 * The final step in the definition of a field template in the index schema,
 * where options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface IndexSchemaFieldTemplateOptionsStep<S extends IndexSchemaFieldTemplateOptionsStep<?>> {

	/**
	 * Restrict the field template to only fields whose path matches the given glob pattern.
	 * <p>
	 * Calling this method multiple times will only erase previously defined globs.
	 *
	 * @param pathGlob A glob pattern that paths must match.
	 * The wildcard {@code *} can be used to represent any string.
	 * The pattern is relative to the index schema element on which this template was created.
	 * @return {@code this}, for method chaining.
	 */
	S matchingPathGlob(String pathGlob);

	/**
	 * Mark the field as multi-valued.
	 * <p>
	 * This informs the backend that this field may contain multiple values for a single parent document or object.
	 * @return {@code this}, for method chaining.
	 */
	S multiValued();

}
