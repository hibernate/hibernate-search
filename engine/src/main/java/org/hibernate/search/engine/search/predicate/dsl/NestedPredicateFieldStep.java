/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.backend.types.ObjectStructure;

/**
 * The initial step in a "nested" predicate definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <N> The type of the next step.
 * @deprecated Use {@link SearchPredicateFactory#nested(String)} instead.
 */
@Deprecated(since = "6.2")
public interface NestedPredicateFieldStep<SR, N extends NestedPredicateNestStep<SR, ?>> {

	/**
	 * Set the object field to "nest" on.
	 * <p>
	 * The selected field must have a {@link ObjectStructure#NESTED nested structure} in the targeted indexes.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the object field.
	 * @return The next step.
	 */
	N objectField(String fieldPath);

}
