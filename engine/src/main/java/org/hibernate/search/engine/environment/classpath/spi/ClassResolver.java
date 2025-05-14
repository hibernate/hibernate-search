/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;


import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A resolver of Java classes.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface ClassResolver {
	/**
	 * Locate a class by name.
	 *
	 * @param className The name of the class to locate
	 * @return The class reference
	 *
	 * @throws ClassLoadingException Indicates the class could not be found
	 */
	Class<?> classForName(String className);

	/**
	 * Locate a pacakge by name.
	 *
	 * @param packageName The name of the package  to locate
	 * @return The package reference
	 *
	 * @throws ClassLoadingException Indicates the class could not be found
	 */
	@Incubating
	Package packageForName(String packageName);

}
