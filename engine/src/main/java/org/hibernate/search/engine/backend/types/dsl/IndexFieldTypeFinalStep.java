/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

import org.hibernate.search.engine.backend.types.IndexFieldType;

/**
 * The final step in an index field type definition, where the type can be retrieved.
 *
 * @param <F> The Java type of values held by fields.
 */
public interface IndexFieldTypeFinalStep<F> {

	/**
	 * Create an {@link IndexFieldType} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link IndexFieldType} resulting from the previous DSL steps.
	 */
	IndexFieldType<F> toIndexFieldType();

}
