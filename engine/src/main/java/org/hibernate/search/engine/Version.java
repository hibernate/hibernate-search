/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Version {

	private Version() {
		// now allowed
	}

	public static String getVersionString() {
		return "[WORKING]";
	}

	static {
		LoggerFactory.make().version( getVersionString() );
	}

	public static void touch() {
	}

	/**
	 * Returns the Java release for the current runtime
	 *
	 * @return the Java release as an integer (e.g. 8 for Java 8)
	 */
	public static int getJavaRelease() {
		// Will return something like 1.8
		String[] specificationVersion = System.getProperty( "java.specification.version" ).split( "\\." );

		return Integer.parseInt( specificationVersion[1] );
	}

}
