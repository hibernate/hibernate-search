/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.classpath.spi;

import java.io.InputStream;
import java.net.URL;

/**
 * A resolver of Java resources.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface ResourceResolver {

	/**
	 * Locate a resource by name (classpath lookup).
	 *
	 * @param name The resource name.
	 *
	 * @return The located URL; may return {@code null} to indicate the resource was not found
	 */
	URL locateResource(String name);

	/**
	 * Locate a resource by name (classpath lookup) and gets its stream.
	 *
	 * @param name The resource name.
	 *
	 * @return The stream of the located resource; may return {@code null} to indicate the resource was not found
	 */
	InputStream locateResourceStream(String name);
}
