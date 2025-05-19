/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.definition;

import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link TypedPredicateDefinition#create(TypedPredicateDefinitionContext)}.
 * @param <SR> Scope root type.
 * @see TypedPredicateDefinition#create(TypedPredicateDefinitionContext)
 */
@Incubating
public interface TypedPredicateDefinitionContext<SR> extends PredicateDefinitionContext {

	/**
	 * @return A predicate factory.
	 * If the named predicate was registered on an object field,
	 * this factory expects field paths to be provided relative to that same object field.
	 * This factory is only valid in the present context and must not be used after
	 * {@link PredicateDefinition#create(PredicateDefinitionContext)} returns.
	 * @see TypedSearchPredicateFactory
	 */
	TypedSearchPredicateFactory<SR> predicate();

}
