/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.io.InputStream;

/**
 * A resolver of Java resources.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface ResourceResolver {

	/**
	 * Locate a resource by name (classpath lookup) and get its stream.
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	InputStream locateResourceStream(String name);
}
