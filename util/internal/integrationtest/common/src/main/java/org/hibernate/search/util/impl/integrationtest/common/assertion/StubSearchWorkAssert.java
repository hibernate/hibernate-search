/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.fail;

import java.util.Objects;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

public class StubSearchWorkAssert {

	private static final String NEWLINE = "\n\t";

	public static StubSearchWorkAssert assertThatSearchWork(StubSearchWork work) {
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
		boolean mismatch = checkForMismatch( builder, "routingKeys", expected.getRoutingKeys(), actual.getRoutingKeys() );
		hasAnyMismatch = mismatch;

		mismatch = checkForMismatch( builder, "truncateAfterTimeout", expected.getTruncateAfterTimeout(), actual.getTruncateAfterTimeout() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "truncateAfterTimeUnit", expected.getTruncateAfterTimeUnit(), actual.getTruncateAfterTimeUnit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "failAfterTimeout", expected.getFailAfterTimeout(), actual.getFailAfterTimeout() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "failAfterTimeUnit", expected.getFailAfterTimeUnit(), actual.getFailAfterTimeUnit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "offset", expected.getOffset(), actual.getOffset() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "limit", expected.getLimit(), actual.getLimit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		if ( hasAnyMismatch ) {
			fail( builder.toString() );
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
