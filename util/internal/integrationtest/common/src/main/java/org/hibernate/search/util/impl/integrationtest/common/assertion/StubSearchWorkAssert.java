/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Objects;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;

import org.junit.Assert;

public class StubSearchWorkAssert {

	private static final String NEWLINE = "\n\t";

	public static StubSearchWorkAssert assertThat(StubSearchWork work) {
		return new StubSearchWorkAssert( work );
	}

	private final StubSearchWork actual;

	private String messageBase = "Search work did not match: ";

	private StubSearchWorkAssert(StubSearchWork actual) {
		this.actual = actual;
	}

	public StubSearchWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubSearchWorkAssert matches(StubSearchWork expected) {
		StringBuilder builder = new StringBuilder( messageBase );

		boolean hasAnyMismatch;
		boolean mismatch = checkForMismatch( builder, "resultType", expected.getResultType(), actual.getResultType() );
		hasAnyMismatch = mismatch;

		mismatch = checkForMismatch( builder, "routingKeys", expected.getRoutingKeys(), actual.getRoutingKeys() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "timeout", expected.getTimeout(), actual.getTimeout() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		if ( expected.getTimeout() != null ) {
			// check only if there's a timeout, otherwise the attribute does not make any sense
			mismatch = checkForMismatch( builder, "timeUnit", expected.getTimeUnit(), actual.getTimeUnit() );
			hasAnyMismatch = hasAnyMismatch || mismatch;
		}

		mismatch = checkForMismatch( builder, "offset", expected.getOffset(), actual.getOffset() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "limit", expected.getLimit(), actual.getLimit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		if ( hasAnyMismatch ) {
			Assert.fail( builder.toString() );
		}

		return this;
	}

	private static boolean checkForMismatch(StringBuilder builder, String name, Object expected, Object actual) {
		if ( !Objects.equals( expected, actual ) ) {
			builder.append( NEWLINE ).append( name ).append( ": " )
					.append( "expected: " ).append( expected )
					.append( ", " ).append( "actual: " ).append( actual );
			return true;
		}
		else {
			return false;
		}

	}
}
