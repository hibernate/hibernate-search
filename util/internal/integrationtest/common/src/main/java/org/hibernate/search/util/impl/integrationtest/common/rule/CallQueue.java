/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.junit.Assert.fail;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.assertj.core.api.Fail;

public class CallQueue<C extends Call<? super C>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public interface Settings {
		boolean allowDuplicates();
	}

	private final Settings settings;

	private final Deque<C> callsExpectedInOrder = new LinkedList<>();
	private C lastMatchingCallInOrder;
	private final List<C> callsExpectedOutOfOrder = new ArrayList<>();
	private final List<C> lastMatchingCallsOutOfOrder = new ArrayList<>();
	private C lastMatchingCall;
	private volatile AssertionError lastVerifyFailure;

	public CallQueue(Settings settings) {
		this.settings = settings;
	}

	public void reset() {
		callsExpectedInOrder.clear();
		lastMatchingCallInOrder = null;
		callsExpectedOutOfOrder.clear();
		lastMatchingCallsOutOfOrder.clear();
		lastMatchingCall = null;
		lastVerifyFailure = null;
	}

	public void expectInOrder(C expectedCall) {
		log.tracef( "Expecting %s", expectedCall );
		callsExpectedInOrder.addLast( expectedCall );
	}

	public void expectOutOfOrder(C expectedCall) {
		log.tracef( "Expecting %s", expectedCall );
		callsExpectedOutOfOrder.add( expectedCall );
	}

	public final synchronized <C2 extends C, T> T verify(C2 actualCall, BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			Function<C2, T> noExpectationBehavior) {
		try {
			log.tracef( "Verifying %s", actualCall );
			return tryVerify( actualCall, callVerifyFunction, noExpectationBehavior );
		}
		catch (AssertionError e) {
			lastVerifyFailure = e;
			throw e;
		}
	}

	private synchronized <C2 extends C, T> T tryVerify(C2 actualCall, BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			Function<C2, T> noExpectationBehavior) {
		boolean allowDuplicates = settings.allowDuplicates();

		CallBehavior<T> behavior;
		List<AssertionError> matchingErrors = new ArrayList<>();

		// First try to match against the calls expected in order
		behavior = tryMatchInOrder( callsExpectedInOrder, actualCall, callVerifyFunction, matchingErrors );
		if ( behavior != null ) {
			return behavior.execute();
		}

		// If the above failed, try to match against the calls expected out of order
		behavior = tryMatchOutOfOrder( callsExpectedOutOfOrder, actualCall, callVerifyFunction, matchingErrors );
		if ( behavior != null ) {
			return behavior.execute();
		}

		List<AssertionError> duplicateCallsMatchingErrors = new ArrayList<>();
		// Maybe this is just a duplicate call?
		// If duplicate calls are allowed, try to match against last matching calls.
		if ( allowDuplicates ) {
			behavior = tryMatchInOrder( lastMatchingCallInOrder, actualCall, callVerifyFunction, duplicateCallsMatchingErrors );
			if ( behavior != null ) {
				return behavior.execute();
			}

			behavior = tryMatchOutOfOrder( lastMatchingCallsOutOfOrder, actualCall, callVerifyFunction,
					duplicateCallsMatchingErrors );
			if ( behavior != null ) {
				return behavior.execute();
			}
		}

		if ( callsExpectedInOrder.isEmpty() && callsExpectedOutOfOrder.isEmpty() ) {
			// No match, not even "duplicate" call matches,
			// but then there were no expectations to begin with.
			// Just apply "noExpectationBehavior".
			return noExpectationBehavior.apply( actualCall );
		}

		if ( !matchingErrors.isEmpty() || !duplicateCallsMatchingErrors.isEmpty() ) {
			// We found similar calls, but they didn't match
			StringBuilder failureMessage = new StringBuilder(
					"Expected call doesn't match expectations, see below for details.\nLast matching call was "
							+ lastMatchingCall
			);
			if ( !matchingErrors.isEmpty() ) {
				failureMessage.append( "\n----------------------------------------" );
				failureMessage.append( "\nFailed matching attempts against expected future calls:\n" );
				for ( AssertionError matchingError : matchingErrors ) {
					failureMessage.append( "\n" ).append( matchingError.getMessage() );
				}
			}
			if ( !duplicateCallsMatchingErrors.isEmpty() ) {
				failureMessage.append( "\n----------------------------------------" );
				failureMessage.append( "\nFailed matching attempts against previous calls"
						+ " (in case this call was just a duplicate):\n" );
				for ( AssertionError matchingError : duplicateCallsMatchingErrors ) {
					failureMessage.append( "\n" ).append( matchingError.getMessage() );
				}
			}
			failureMessage.append( "\n----------------------------------------" );
			throw createFailure( failureMessage.toString() );
		}
		else {
			// We didn't find any similar call
			throw createFailure( "Unexpected call: " + actualCall + "; details:\n"
					+ new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() ).value( actualCall ) );
		}
	}

	private <C2 extends C, T> CallBehavior<T> tryMatchInOrder(Deque<C> callsExpected, C2 actualCall,
			BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			List<AssertionError> matchingErrors) {
		if ( callsExpected.isEmpty() ) {
			return null;
		}
		return tryMatchInOrder( callsExpected.getFirst(), actualCall, callVerifyFunction, matchingErrors );
	}

	private <C2 extends C, T> CallBehavior<T> tryMatchInOrder(C expectedCall, C2 actualCall,
			BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			List<AssertionError> matchingErrors) {
		if ( expectedCall == null ) {
			return null;
		}
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
			lastMatchingCallInOrder = expectedCall;
			lastMatchingCall = expectedCall;
			return behavior;
		}
		return null;
	}

	private <C2 extends C, T> CallBehavior<T> tryMatchOutOfOrder(List<C> callsExpected, C2 actualCall,
			BiFunction<C, C2, CallBehavior<T>> callVerifyFunction,
			List<AssertionError> matchingErrors) {
		if ( callsExpected.isEmpty() ) {
			return null;
		}
		for ( C expectedCall : callsExpected ) {
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
					callsExpected.remove( expectedCall );
					lastMatchingCallsOutOfOrder.add( expectedCall );
					lastMatchingCall = expectedCall;
					return behavior;
				}
			}
		}
		return null;
	}

	private AssertionError createFailure(String message) {
		try {
			Fail.fail( message );
		}
		catch (AssertionError e) {
			return e;
		}
		// Dead code, the code above always throws an exception
		throw new IllegalStateException( "This should not happen" );
	}

	public synchronized void verifyNoUnexpectedCall() {
		if ( lastVerifyFailure != null ) {
			Fail.fail(
					"A verify error occurred during the test: " + lastVerifyFailure.getMessage(),
					lastVerifyFailure
			);
		}
	}

	public synchronized void verifyExpectationsMet() {
		List<C> remaining = new ArrayList<>();
		remaining.addAll( callsExpectedInOrder );
		remaining.addAll( callsExpectedOutOfOrder );
		if ( !remaining.isEmpty() ) {
			fail( "Missing call: expected " + remaining + "; details:\n"
					+ new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() ).value( remaining ) );
		}
	}

	public synchronized long remainingExpectedCallCount() {
		return ( (long) callsExpectedInOrder.size() ) + callsExpectedOutOfOrder.size();
	}

}
