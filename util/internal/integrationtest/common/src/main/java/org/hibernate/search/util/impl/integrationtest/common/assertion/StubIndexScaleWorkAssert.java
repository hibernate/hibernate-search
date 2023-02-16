/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.junit.Assert.fail;

import java.util.Objects;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScaleWork;

public class StubIndexScaleWorkAssert {

	public static StubIndexScaleWorkAssert assertThatIndexScaleWork(StubIndexScaleWork work) {
		return new StubIndexScaleWorkAssert( work );
	}

	private final StubIndexScaleWork actual;

	private String messageBase = "Index-scale work did not match: ";

	private StubIndexScaleWorkAssert(StubIndexScaleWork actual) {
		this.actual = actual;
	}

	public StubIndexScaleWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubIndexScaleWorkAssert matches(StubIndexScaleWork expected) {
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );

		builder.startObject();

		boolean hasAnyMismatch;
		boolean mismatch = checkForMismatch( builder, "type", expected.getType(), actual.getType() );
		hasAnyMismatch = mismatch;
		mismatch = checkForMismatch( builder, "tenantIdentifiers",
				expected.getTenantIdentifiers(), actual.getTenantIdentifiers()
		);
		hasAnyMismatch = hasAnyMismatch || mismatch;
		mismatch = checkForMismatch( builder, "routingKeys",
				expected.getRoutingKeys(), actual.getRoutingKeys()
		);
		hasAnyMismatch = hasAnyMismatch || mismatch;

		builder.endObject();

		if ( hasAnyMismatch ) {
			fail( messageBase + builder.toString() );
		}

		return this;
	}

	private static boolean checkForMismatch(ToStringTreeBuilder builder, String name, Object expected, Object actual) {
		if ( !Objects.equals( expected, actual ) ) {
			builder.startObject( name );
			builder.attribute( "expected", expected );
			builder.attribute( "actual", actual );
			builder.endObject();
			return true;
		}
		else {
			return false;
		}

	}
}
