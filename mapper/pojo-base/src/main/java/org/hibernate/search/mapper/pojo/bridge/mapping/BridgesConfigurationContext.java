/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping;

import org.hibernate.search.util.common.annotation.Incubating;

public interface BridgesConfigurationContext {

	/**
	 * Define default bridges or binders for properties with the exact given type.
	 * <p>
	 * Properties with a supertype or subtype of the given type will not match this definition.
	 *
	 * @param clazz The raw type to match.
	 * @param <T> The raw type to match.
	 * @return The initial step of a DSL where the default bridges can be defined for properties with a matching type.
	 */
	<T> DefaultBridgeDefinitionStep<?, T> exactType(Class<T> clazz);

	/**
	 * Define default binders for properties with the given type or a subtype.
	 * <p>
	 * Properties with a supertype of the given type will not match this definition.
	 *
	 * @param clazz The raw type to match.
	 * @param <T> The raw type to match.
	 * @return The initial step of a DSL where the default bridges can be defined for properties with a matching type.
	 */
	@Incubating
	<T> DefaultBinderDefinitionStep<?> subTypesOf(Class<T> clazz);

	/**
	 * Define default binders for properties of a subtype of the given type.
	 * <p>
	 * Properties with the given type or with a supertype of the given type will not match this definition.
	 * <p>
	 * Useful to define a binder for an abstract type that cannot be bound, but whose concrete subtypes can be bound,
	 * such as {@code Enum.class}.
	 *
	 * @param clazz The raw type to match.
	 * @param <T> The raw type to match.
	 * @return The initial step of a DSL where the default bridges can be defined for properties with a matching type.
	 */
	@Incubating
	<T> DefaultBinderDefinitionStep<?> strictSubTypesOf(Class<T> clazz);

}
