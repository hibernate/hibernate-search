/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Assert;

import org.assertj.core.api.Fail;

public class CallQueue<C extends Call<? super C>> {

	private final Deque<C> callsExpectedInOrder = new LinkedList<>();
	private final List<C> callsExpectedOutOfOrder = new ArrayList<>();
	private C lastMatchingCall;
	private AssertionError lastVerifyFailure;

	public void reset() {
		callsExpectedInOrder.clear();
		callsExpectedOutOfOrder.clear();
		lastMatchingCall = null;
	}

	public void expectInOrder(C expectedCall) {
		callsExpectedInOrder.addLast( expectedCall );
	}

	public void expectOutOfOrder(C expectedCall) {
		callsExpectedOutOfOrder.add( expectedCall );
	}

	public <C2 extends C, T> T verify(C2 actualCall, BiFunction<C, C2, CallBehavior<T>> callVerifyFunction) {
		return verify(
				actualCall,
				callVerifyFunction,
				call -> {
					Assert.fail( "No call expected, but got: " + call );
					// Dead code, we throw an exception above
					return null;
				}
		);
	}

	public synchronized <C2 extends C, T> T verify(C2 actualCall, BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			Function<C2, T> noExpectationBehavior) {
		if ( callsExpectedInOrder.isEmpty() && callsExpectedOutOfOrder.isEmpty() ) {
			return noExpectationBehavior.apply( actualCall );
		}

		List<AssertionError> matchingErrors = new ArrayList<>();

		// First try to match against the calls expected in order
		if ( !callsExpectedInOrder.isEmpty() ) {
			C expectedCall = callsExpectedInOrder.getFirst();
			CallBehavior<T> behavior = null;
			try {
				behavior = callVerifyFunction.apply( expectedCall, actualCall );
			}
			catch (AssertionError e) {
				// No match. Save the error for later and check the out-of-order expected calls.
				matchingErrors.add( e );
			}
			if ( behavior != null ) {
				// MATCH! Stop looking for a matching call.
				callsExpectedInOrder.remove( expectedCall );
				lastMatchingCall = expectedCall;
				return behavior.execute();
			}
		}

		// If the above failed, try to match against the calls expected out of order
		if ( !callsExpectedOutOfOrder.isEmpty() ) {
			for ( C expectedCall : callsExpectedOutOfOrder ) {
				if ( expectedCall.isSimilarTo( actualCall ) ) {
					// The call looks similar, let's try to match against it...
					CallBehavior<T> behavior = null;
					try {
						behavior = callVerifyFunction.apply( expectedCall, actualCall );
					}
					catch (AssertionError e) {
						// No match. Save the error for later and continue the loop.
						matchingErrors.add( e );
					}
					if ( behavior != null ) {
						// MATCH! Stop looking for a matching call.
						callsExpectedOutOfOrder.remove( expectedCall );
						lastMatchingCall = expectedCall;
						return behavior.execute();
					}
				}
			}
		}

		if ( !matchingErrors.isEmpty() ) {
			// We found similar calls, but they didn't match
			StringBuilder failureMessage = new StringBuilder(
					"Unexpected call, see below for details.\n\tLast matching call was "
							+ lastMatchingCall + "\n\tFailed matching attempts for this call: "
			);
			for ( AssertionError matchingError : matchingErrors ) {
				failureMessage.append( "\n" ).append( matchingError.getMessage() );
			}
			throw createFailure( failureMessage.toString() );
		}
		else {
			// We didn't find any similar call
			throw createFailure( "Unexpected call: " + actualCall );
		}
	}

	private AssertionError createFailure(String message) {
		try {
			Fail.fail( message );
		}
		catch (AssertionError e) {
			lastVerifyFailure = e;
			return e;
		}
		// Dead code, the code above always throws an exception
		throw new IllegalStateException( "This should not happen" );
	}

	public synchronized void verifyExpectationsMet() {
		if ( lastVerifyFailure != null ) {
			Fail.fail(
					"A verify error occurred during the test: " + lastVerifyFailure.getMessage(),
					lastVerifyFailure
			);
		}

		List<C> remaining = new ArrayList<>();
		remaining.addAll( callsExpectedInOrder );
		remaining.addAll( callsExpectedOutOfOrder );
		if ( !remaining.isEmpty() ) {
			Assert.fail( "Expected " + remaining );
		}
	}

}
