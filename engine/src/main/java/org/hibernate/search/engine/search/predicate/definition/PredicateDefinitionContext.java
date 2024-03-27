/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.definition;

import java.util.Optional;

import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link PredicateDefinition#create(PredicateDefinitionContext)}.
 * @see PredicateDefinition#create(PredicateDefinitionContext)
 */
@Incubating
public interface PredicateDefinitionContext {

	/**
	 * @return A predicate factory.
	 * If the named predicate was registered on an object field,
	 * this factory expects field paths to be provided relative to that same object field.
	 * This factory is only valid in the present context and must not be used after
	 * {@link PredicateDefinition#create(PredicateDefinitionContext)} returns.
	 * @see SearchPredicateFactory
	 */
	SearchPredicateFactory predicate();

	/**
	 * @param name The name of the parameter.
	 * @return The value provided to {@link NamedPredicateOptionsStep#param(String, Object)} for this parameter.
	 * @throws SearchException If no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 * @deprecated Use {@link #param(String, Class)} instead.
	 */
	@Deprecated
	default Object param(String name) {
		return param( name, Object.class );
	}

	/**
	 * @param name The name of the parameter.
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return The value provided to {@link NamedPredicateOptionsStep#param(String, Object)} for this parameter.
	 * @throws SearchException If no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 */
	<T> T param(String name, Class<T> paramType);

	/**
	 * @param name The name of the parameter.
	 * @return An optional containing the value provided to {@link NamedPredicateOptionsStep#param(String, Object)}
	 * for this parameter, or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 * @deprecated Use {@link #paramOptional(String, Class)} instead.
	 */
	@Deprecated
	default Optional<Object> paramOptional(String name) {
		return paramOptional( name, Object.class );
	}

	/**
	 * @param name The name of the parameter.
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return An optional containing the value provided to {@link NamedPredicateOptionsStep#param(String, Object)}
	 * for this parameter, or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see NamedPredicateOptionsStep#param(String, Object)
	 */
	<T> Optional<T> paramOptional(String name, Class<T> paramType);

}
