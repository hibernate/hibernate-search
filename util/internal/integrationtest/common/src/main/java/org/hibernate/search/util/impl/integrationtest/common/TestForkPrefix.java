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
			parallelForks = true;
		}

		// TODO drop test.parallel.modules.enabled once the bytecode enhancement engine supports forkCount>1
		//  for ORM modules — at that point forkCount alone will be sufficient for fork isolation.
		boolean parallelModules = Boolean.parseBoolean(
				System.getProperty( "test.parallel.modules.enabled", "false" ) );

		boolean needsIsolation = parallelForks || parallelModules;

		String forkNumber = System.getProperty( "test.fork.number", System.getProperty( "surefire.forkNumber", "" ) );
		if ( needsIsolation && ( forkNumber.isBlank() || !isNumber( forkNumber ) ) ) {
			throw new IllegalStateException( "Test Fork number must be specified." );
		}

		PREFIX = needsIsolation ? "fork_" + forkNumber + "_" : "";
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
