/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

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
		return Version.class.getPackage().getImplementationVersion();
	}

	/**
	 * @return A string representation of the version of Hibernate Search.
	 * @deprecated Use {@link #versionString()} instead.
	 */
	@Deprecated
	public static String getVersionString() {
		return versionString();
	}

}
