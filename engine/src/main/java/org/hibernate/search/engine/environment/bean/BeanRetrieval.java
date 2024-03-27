/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean;

/**
 * Defines where beans are retrieved from.
 */
public enum BeanRetrieval {

	/**
	 * Retrieve a built-in bean.
	 * <p>
	 * Retrieve the bean from a registry defined using
	 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurer}s.
	 * If a name is provided, it is interpreted as the name passed to
	 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext#define(Class, String, BeanReference)}.
	 */
	BUILTIN,
	/**
	 * Retrieve an actual managed bean.
	 * <p>
	 * Retrieve the bean from the bean manager (e.g. CDI, Spring, ...).
	 * If a name is provided, it is interpreted as a bean name,
	 * for example assigned through {@code jakarta.inject.Named}.
	 */
	BEAN,
	/**
	 * Retrieve an instance of a class.
	 * <p>
	 * First, attempt to retrieve the bean from the bean manager, (e.g. CDI, Spring, ...).
	 * Failing that, instantiate the bean using reflection, through its public, no-arg constructor.
	 * If a name is provided, it is interpreted as a class name.
	 */
	CLASS,
	/**
	 * Retrieve an instance of a class through the constructor directly, ignoring any bean manager.
	 * <p>
	 * Instantiate the bean using reflection, through its public, no-arg constructor.
	 * If a name is provided, it is interpreted as a class name.
	 */
	CONSTRUCTOR,
	/**
	 * Retrieve a bean using any available method.
	 * <p>
	 * Attempts are made in the following order:
	 * <ol>
	 *     <li>Retrieve a pre-configured bean (see {@link #BUILTIN})</li>
	 *     <li>Retrieve an actual managed bean, interpreting the name (if any) as the bean name (see {@link #BEAN})</li>
	 *     <li>Retrieve an actual managed bean, interpreting the name (if any) as the class name (see {@link #CLASS})</li>
	 *     <li>Retrieve an instance of a class through reflection,
	 *     interpreting the name (if any) as the class name (see {@link #CONSTRUCTOR})</li>
	 * </ol>
	 */
	ANY

}
