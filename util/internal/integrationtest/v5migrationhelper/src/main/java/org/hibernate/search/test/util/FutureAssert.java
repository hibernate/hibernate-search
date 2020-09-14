/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.Assert;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;

/**
 * @author Yoann Rodiere
 */
public class FutureAssert<T> extends AbstractObjectAssert<FutureAssert<T>, Future<T>> {

	public static <T> FutureAssert<T> assertThat(Future<T> future) {
		return new FutureAssert<>( future );
	}

	protected FutureAssert(Future<T> actual) {
		super( actual, FutureAssert.class );
	}

	public FutureAssert<T> isPending() {
		try {
			Object result = getNow();
			failWithMessage( "future <%s> should be pending, but instead it succeeded with result <%s>", actual, result );
		}
		catch (TimeoutException e) {
			// All's good
		}
		catch (CancellationException e) {
			failWithCauseAndMessage( e, "future <%s> should be pending, but instead it's been cancelled", actual, e );
		}
		catch (ExecutionException e) {
			failWithCauseAndMessage( e, "future <%s> should be pending, but instead it failed with exception: %s", actual, e );
		}
		return this;
	}

	public FutureAssert<T> isSuccessful() {
		return isSuccessful( value -> {
		} );
	}

	public FutureAssert<T> isSuccessful(T expectedValue) {
		return isSuccessful( value -> Assertions.assertThat( expectedValue ).isEqualTo( expectedValue ) );
	}

	public FutureAssert<T> isSuccessful(Consumer<T> valueAssertion) {
		try {
			T result = getNow();
			try {
				valueAssertion.accept( result );
			}
			catch (AssertionError e2) {
				failWithCauseAndMessage( e2, "future <%s> succeeded as expected, but the result is wrong: %s", actual, e2 );
			}
		}
		catch (TimeoutException e) {
			failWithMessage( "future <%s> should have succeeded, but instead it's still pending", actual );
		}
		catch (CancellationException e) {
			failWithCauseAndMessage( e, "future <%s> should have succeeded, but instead it's been cancelled", actual, e );
		}
		catch (ExecutionException e) {
			failWithCauseAndMessage( e, "future <%s> should have succeeded, but instead it failed with exception: %s", actual, e );
		}
		return this;
	}

	public FutureAssert<T> isFailed() {
		return isFailed( throwable -> { } );
	}

	public FutureAssert<T> isFailed(Throwable expectedThrowable) {
		return isFailed( throwable -> Assertions.assertThat( throwable ).isEqualTo( expectedThrowable ) );
	}

	public FutureAssert<T> isFailed(Matcher<? super Throwable> exceptionMatcher) {
		return isFailed( throwable -> Assert.assertThat( throwable, exceptionMatcher ) );
	}

	public FutureAssert<T> isFailed(Consumer<Throwable> exceptionAssertion) {
		Object result;
		try {
			result = getNow();
			failWithMessage( "future <%s> should have failed, but instead it succeeded with result <%s>", actual, result );
		}
		catch (TimeoutException e) {
			failWithMessage( "future <%s> should have failed, but instead it's still pending", actual );
		}
		catch (CancellationException e) {
			failWithCauseAndMessage( e, "future <%s> should have failed, but instead it's been cancelled", actual, e );
		}
		catch (ExecutionException e) {
			try {
				exceptionAssertion.accept( e.getCause() );
			}
			catch (AssertionError e2) {
				failWithCauseAndMessage( e2, "future <%s> failed as expected, but the exception is wrong: %s", actual, e2 );
			}
		}
		return this;
	}

	private T getNow() throws TimeoutException, CancellationException, ExecutionException {
		try {
			return actual.get( 0, TimeUnit.NANOSECONDS );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( "Interrupted while getting a future value with a 0 timeout (?)", e );
		}
		catch (TimeoutException | CancellationException e) {
			throw e;
		}
		catch (ExecutionException e) {
			Throwable t = e;
			while ( t != null ) {
				if ( t instanceof AssertionError ) {
					Throwable cause = e.getCause();
					failWithCauseAndMessage( cause, "future <%s> failed because of a failing assertion: %s", actual, cause );
				}
				t = t.getCause();
			}
			throw e;
		}
	}

	protected void failWithCauseAndMessage(Throwable cause, String errorMessage, Object... arguments) {
		try {
			failWithMessage( errorMessage, arguments );
		}
		catch (AssertionError assertionError) {
			assertionError.initCause( cause );
			throw assertionError;
		}
	}
}
