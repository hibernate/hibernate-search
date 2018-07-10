/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test;

import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.junit.Test;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert;

/**
 * A util allowing to run blocks of code ("sub-tests"), expecting them to throw an exception.
 * <p>
 * Useful in particular when expecting an exception for each execution of a loop,
 * in which case {@link org.junit.rules.ExpectedException} or {@link Test#expected()} cannot be used.
 * <p>
 * By default any thrown exception will be accepted; if you want to run additional checks on the thrown exception,
 * use {@link #assertThrown()}.
 */
public class SubTest {

	public static SubTest expectException(Runnable runnable) {
		return expectException( runnable.toString(), runnable );
	}

	public static SubTest expectException(String description, Runnable runnable) {
		return expectException(
				description,
				() -> {
					runnable.run();
					return null;
				}
		);
	}

	public static SubTest expectException(Callable<?> callable) {
		return expectException( callable.toString(), callable );
	}

	public static SubTest expectException(String description, Callable<?> callable) {
		try {
			callable.call();
			fail( "'" + description + "' should have thrown an exception" );
			throw new IllegalStateException( "This should never happen" );
		}
		catch (Exception e) {
			return new SubTest( "Exception thrown by '" + description + "'", e );
		}
	}

	private final String description;

	private final Throwable thrown;

	private SubTest(String description, Throwable thrown) {
		this.description = description;
		this.thrown = thrown;
	}

	public AbstractThrowableAssert<?, Throwable> assertThrown() {
		return new ThrowableAssert( thrown )
				.as( description );
	}

}
