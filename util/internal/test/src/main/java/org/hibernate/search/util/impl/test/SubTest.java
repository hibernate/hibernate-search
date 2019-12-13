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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;

/**
 * A util allowing to run blocks of code as "sub-tests".
 * <p>
 * This class is useful when looping over several executions of the same set of assertions:
 * <ul>
 *     <li>
 *         When executing code that both produces and consumes instances of a different generic type T
 *         for each execution of a loop,
 *         you usually cannot write type-safe code easily, because of limitations and how generics work,
 *         but you can with {@link #expectSuccess(Object, ParameterizedSubTest)}.
 *     </li>
 *     <li>
 *         When expecting an exception for each execution of a loop,
 *         you cannot use {@link org.junit.rules.ExpectedException} or {@link Test#expected()},
 *         but you can use {@link #expectException(String, Runnable)}.
 *         By default any thrown exception will be accepted; if you want to run additional checks on the thrown exception,
 *         use {@link ExceptionThrowingSubTest#assertThrown()}.
 *     </li>
 * </ul>
 */
public class SubTest {

	private static final BasicLogger log = Logger.getLogger( SubTest.class.getName() );

	public static void expectSuccessAfterRetry(FlakySubTest subTest) {
		Throwable failure = null;
		for ( int i = 0; i < 3; i++ ) {
			try {
				subTest.test();
				return; // Test succeeded
			}
			catch (AssertionError|Exception e) {
				if ( failure == null ) {
					failure = e;
				}
				else {
					failure.addSuppressed( e );
				}
			}
		}
		Assertions.fail(
				"Test failed after 3 attempts",
				failure
		);
	}

	public static <T> void expectSuccess(T parameter, ParameterizedSubTest<T> subTest) {
		subTest.test( parameter );
	}

	public static ExceptionThrowingSubTest expectException(Runnable runnable) {
		return expectException( runnable.toString(), runnable );
	}

	public static ExceptionThrowingSubTest expectException(String description, Runnable runnable) {
		return expectException(
				description,
				() -> {
					runnable.run();
					return null;
				}
		);
	}

	public static ExceptionThrowingSubTest expectException(Callable<?> callable) {
		return expectException( callable.toString(), callable );
	}

	public static ExceptionThrowingSubTest expectException(String description, Callable<?> callable) {
		try {
			callable.call();
		}
		catch (Exception e) {
			log.infof( e, "SubTest caught exception (an exception was expected; type/message checks will follow):", e );
			return new ExceptionThrowingSubTest( "Exception thrown by '" + description + "'", e );
		}
		fail( "'" + description + "' should have thrown an exception" );
		throw new IllegalStateException( "This should never happen" );
	}

	public static ExceptionThrowingSubTest expectThrowable(Runnable runnable) {
		return expectThrowable( runnable.toString(), runnable );
	}

	public static ExceptionThrowingSubTest expectThrowable(String description, Runnable runnable) {
		return expectThrowable(
				description,
				() -> {
					runnable.run();
					return null;
				}
		);
	}

	public static ExceptionThrowingSubTest expectThrowable(Callable<?> callable) {
		return expectThrowable( callable.toString(), callable );
	}

	public static ExceptionThrowingSubTest expectThrowable(String description, Callable<?> callable) {
		try {
			callable.call();
		}
		catch (Throwable e) {
			log.infof( e, "SubTest caught throwable (a throwable was expected; type/message checks will follow):", e );
			return new ExceptionThrowingSubTest( "Exception thrown by '" + description + "'", e );
		}
		fail( "'" + description + "' should have thrown a throwable" );
		throw new IllegalStateException( "This should never happen" );
	}

	private SubTest() {
	}

	public static final class ExceptionThrowingSubTest {
		private final String description;

		private final Throwable thrown;

		private ExceptionThrowingSubTest(String description, Throwable thrown) {
			this.description = description;
			this.thrown = thrown;
		}

		public AbstractThrowableAssert<?, Throwable> assertThrown() {
			return new ThrowableAssert( thrown )
					.as( description );
		}
	}

	@FunctionalInterface
	public interface FlakySubTest {
		void test() throws Exception;
	}

	@FunctionalInterface
	public interface ParameterizedSubTest<T> {
		void test(T param);
	}

}
