/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		// This implementation is replaced during the build with another one that returns the correct value.
		return "UNKNOWN";
	}

}
