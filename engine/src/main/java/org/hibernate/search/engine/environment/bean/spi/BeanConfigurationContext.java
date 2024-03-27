/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface BeanConfigurationContext {

	/**
	 * Define a way to resolve a bean referenced by its {@code exposedType}.
	 * <p>
	 * Affects the behavior of {@link BeanResolver#resolve(Class, org.hibernate.search.engine.environment.bean.BeanRetrieval)}
	 * in particular.
	 *
	 * @param exposedType The type that this definition will match (exact match: inheritance is ignored).
	 * @param reference The reference to the bean.
	 * This reference should generally call the bean's constructor directly without relying on the bean resolver.
	 * However, the reference can also rely on the bean resolver to resolve a reference,
	 * provided that reference is not {@code BeanReference.of( exposedType )} (which would create a cycle).
	 * @param <T> The exposed type of the bean.
	 */
	<T> void define(Class<T> exposedType, BeanReference<? extends T> reference);

	/**
	 * Define a way to resolve a bean referenced by its {@code exposedType} and {@code name}.
	 * <p>
	 * Affects the behavior of {@link BeanResolver#resolve(Class, String, org.hibernate.search.engine.environment.bean.BeanRetrieval)}
	 * in particular.
	 *
	 * @param exposedType The type that this definition will match (exact match: inheritance is ignored).
	 * @param name The name that this definition will match (exact match: case is taken into account).
	 * @param reference The reference to the bean.
	 * This reference should generally call the bean's constructor directly without relying on the bean resolver.
	 * However, the reference can also rely on the bean resolver to resolve a reference,
	 * provided that reference is not {@code BeanReference.of( exposedType, name )} (which would create a cycle).
	 * @param <T> The exposed type of the bean.
	 */
	<T> void define(Class<T> exposedType, String name, BeanReference<? extends T> reference);

}
