/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Version {

	private Version() {
		//now allowed
	}

	public static String getVersionString() {
		return Version.class.getPackage().getImplementationVersion();
	}

	/**
	 * Logs the Hibernate Search version (using {@link #getVersionString()}) to the logging system.
	 */
	public static void logVersion() {
		LoggerFactory.make( Log.class, MethodHandles.lookup() ).version( getVersionString() );
	}
}
