/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test;

import org.assertj.core.api.Assertions;

/**
 * A util allowing to run blocks of code as "sub-tests".
 * <p>
 * This class is useful when looping over several executions of the same set of assertions:
 * when executing code that both produces and consumes instances of a different generic type T
 * for each execution of a loop,
 * you usually cannot write type-safe code easily, because of limitations and how generics work,
 * but you can with {@link #expectSuccess(Object, ParameterizedSubTest)}.
 */
public class SubTest {

	public static void expectSuccessAfterRetry(FlakySubTest subTest) {
		Throwable failure = null;
		for ( int i = 0; i < 3; i++ ) {
			try {
				subTest.test();
				return; // Test succeeded
			}
			catch (AssertionError | Exception e) {
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

	private SubTest() {
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
