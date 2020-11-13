/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
