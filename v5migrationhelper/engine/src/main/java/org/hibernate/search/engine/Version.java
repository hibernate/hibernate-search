/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

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

	static {
		LoggerFactory.make( MethodHandles.lookup() ).version( getVersionString() );
	}

	public static void touch() {
	}
}
