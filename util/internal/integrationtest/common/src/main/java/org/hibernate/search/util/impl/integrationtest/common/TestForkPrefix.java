/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common;

public final class TestForkPrefix {

	public static final String PREFIX;

	static {
		String forkCountStr = System.getProperty( "test.fork.count", "1" );
		boolean parallelForks;
		try {
			parallelForks = Integer.parseInt( forkCountStr ) > 1;
		}
		catch (NumberFormatException e) {
			// forkCount uses Surefire's multiplier format (e.g. "0.5C") — parallel is enabled
			parallelForks = true;
		}
		String forkNumber = System.getProperty( "test.fork.number", System.getProperty( "surefire.forkNumber", "" ) );
		if ( forkNumber.isBlank() && Integer.parseInt( forkNumber ) > -1 ) {
			throw new IllegalStateException( "Test Fork number must be specified." );
		}

		PREFIX = parallelForks ? "fork_" + forkNumber + "_" : "";
	}

	private TestForkPrefix() {
	}
}
