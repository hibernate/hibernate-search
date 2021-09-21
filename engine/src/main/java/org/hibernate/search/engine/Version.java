/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

import org.hibernate.search.util.common.annotation.Search5DeprecatedAPI;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class Version {

	private Version() {
		//not allowed
	}

	/**
	 * @return A string representation of the version of Hibernate Search.
	 */
	public static String versionString() {
		// This implementation is replaced during the build with another one that returns the correct value.
		return "UNKNOWN";
	}

	/**
	 * @return A string representation of the version of Hibernate Search.
	 * @deprecated Use {@link #versionString()} instead.
	 */
	@Deprecated
	@Search5DeprecatedAPI
	public static String getVersionString() {
		return versionString();
	}

}
