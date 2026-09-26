/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

public final class TestForkPrefix {

	public static final String PREFIX;

	static {
		String forkCountStr = System.getProperty( "test.parallel.fork.count", "1" );
		boolean parallelForks;
		try {
			parallelForks = Integer.parseInt( forkCountStr ) > 1;
		}
		catch (NumberFormatException e) {
			parallelForks = true;
		}

		boolean parallelModules = Boolean.parseBoolean(
				System.getProperty( "test.parallel.modules.enabled", "false" ) );

		boolean needsIsolation = parallelForks || parallelModules;

		String forkNumber = System.getProperty( "test.parallel.fork.number", System.getProperty( "surefire.forkNumber", "" ) );
		if ( needsIsolation && ( forkNumber.isBlank() || !isNumber( forkNumber ) ) ) {
			throw new IllegalStateException( "Test Fork number must be specified." );
		}

		String moduleId = System.getProperty( "test.parallel.module.id", "" );
		// Use a hash of the module artifactId to keep the prefix short.
		// PostgreSQL limits database names to 63 characters; full artifactIds easily exceed that.
		String modulePrefix = moduleId.isEmpty()
				? ""
				: Integer.toHexString( moduleId.hashCode() & 0x7fffffff ) + "_";
		PREFIX = needsIsolation ? modulePrefix + "fork_" + forkNumber + "_" : "";
	}

	private static boolean isNumber(String maybeNumber) {
		try {
			Integer.parseInt( maybeNumber );
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	private TestForkPrefix() {
	}
}
