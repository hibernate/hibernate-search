/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.assertion;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.integrationtest.util.common.stub.StubTreeNodeCompare;
import org.hibernate.search.integrationtest.util.common.stub.StubTreeNodeMismatch;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.StubIndexWork;

import junit.framework.AssertionFailedError;

public class StubIndexWorkAssert {

	private static final String NEWLINE = "\n\t";
	private static final String INDENT = "\t";

	public static StubIndexWorkAssert assertThat(StubIndexWork work) {
		return new StubIndexWorkAssert( work );
	}

	private final StubIndexWork actual;

	private String messageBase = "Index work did not match: ";

	private StubIndexWorkAssert(StubIndexWork actual) {
		this.actual = actual;
	}

	public StubIndexWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubIndexWorkAssert matches(StubIndexWork expected) {
		StringBuilder builder = new StringBuilder( messageBase );

		boolean hasAnyMismatch;
		boolean mismatch = checkForMismatch( builder, "type", expected.getType(), actual.getType() );
		hasAnyMismatch = mismatch;
		mismatch = checkForMismatch( builder, "tenantIdentifier",
				expected.getTenantIdentifier(), actual.getTenantIdentifier()
		);
		hasAnyMismatch = hasAnyMismatch || mismatch;
		mismatch = checkForMismatch( builder, "identifier", expected.getIdentifier(), actual.getIdentifier() );
		hasAnyMismatch = hasAnyMismatch || mismatch;
		mismatch = checkForMismatch( builder, "routingKey", expected.getRoutingKey(), actual.getRoutingKey() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		Map<String, StubTreeNodeMismatch> documentMismatches =
				StubTreeNodeCompare.compare( expected.getDocument(), actual.getDocument() );
		if ( !documentMismatches.isEmpty() ) {
			hasAnyMismatch = true;
			builder.append( NEWLINE ).append( "document:" );
			StubTreeNodeCompare.appendTo( builder, documentMismatches, NEWLINE + INDENT, INDENT );
		}

		if ( hasAnyMismatch ) {
			throw new AssertionFailedError( builder.toString() );
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
