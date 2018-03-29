/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.BiFunction;

import org.junit.Assert;

class CallQueue<C> {

	private final Deque<C> expectedQueue = new LinkedList<>();
	private C lastMatchingCall;

	void reset() {
		expectedQueue.clear();
		lastMatchingCall = null;
	}

	void expect(C expectedCall) {
		expectedQueue.addLast( expectedCall );
	}

	<C2 extends C, T> T verify(C2 actualCall, BiFunction<C, C2, T> callVerifyFunction) {
		C expectedCall = expectedQueue.poll();
		try {
			if ( expectedCall == null ) {
				Assert.fail( "No call expected, but got: " + actualCall );
				// Dead code, we throw an exception above
				return null;
			}
			else {
				T result = callVerifyFunction.apply( expectedCall, actualCall );
				lastMatchingCall = actualCall;
				return result;
			}
		}
		catch (AssertionError e) {
			Assert.fail(
					"Unexpected call, see below for details.\n\tLast matching call was "
					+ lastMatchingCall + "\n\tError for this call was: " + e.getMessage()
			);
			// Dead code, we throw an exception above
			throw e;
		}
	}

	void verifyEmpty() {
		C expectedCall = expectedQueue.peek();
		if ( expectedCall != null ) {
			Assert.fail( "Expected " + expectedCall );
		}
	}

}
