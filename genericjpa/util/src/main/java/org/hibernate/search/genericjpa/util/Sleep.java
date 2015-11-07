/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.util;

import java.util.function.BooleanSupplier;

/**
 * @author Martin Braun
 */
public final class Sleep {

	private Sleep() {
		// can't touch this!
	}

	public static void sleep(long millis, BooleanSupplier condition) throws InterruptedException {
		sleep( millis, condition, "no message!" );
	}

	public static void sleep(long millis, BooleanSupplier condition, String message) throws InterruptedException {
		sleep( millis, condition, Math.max( millis / 50, 1 ), message );
	}

	public static void sleep(long millis, BooleanSupplier condition, long delayMillis, String message)
			throws InterruptedException {
		long waited = 0;
		long start = System.currentTimeMillis();
		while ( !condition.getAsBoolean() ) {
			Thread.sleep( delayMillis );
			waited += System.currentTimeMillis() - start;
			if ( waited >= millis ) {
				throw new RuntimeException( "timeout: " + message );
			}
		}
	}

}
