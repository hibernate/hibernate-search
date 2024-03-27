/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.bean.impl;

public enum BeanSource {

	/**
	 * The beans defined using
	 * {@link org.hibernate.search.engine.environment.bean.spi.BeanConfigurer}s.
	 */
	CONFIGURATION,
	/**
	 * The bean manager, e.g. CDI, Spring, ...
	 */
	BEAN_MANAGER,
	/**
	 * The bean manager, e.g. CDI, Spring, ..., but interpreting names as class names instead of bean names.
	 */
	BEAN_MANAGER_ASSUME_CLASS_NAME,
	/**
	 * Reflection, i.e. the public, no-argument constructor of the bean class.
	 */
	REFLECTION

}
