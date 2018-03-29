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
		mismatch = checkForMismatch( builder, "firstResultIndex",
				expected.getFirstResultIndex(), actual.getFirstResultIndex()
		);
		hasAnyMismatch = hasAnyMismatch || mismatch;
		mismatch = checkForMismatch( builder, "maxResultsCount", expected.getMaxResultsCount(), actual.getMaxResultsCount() );
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
