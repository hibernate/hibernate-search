/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Objects;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexScopeWork;

import org.junit.Assert;

public class StubIndexScopeWorkAssert {

	public static StubIndexScopeWorkAssert assertThat(StubIndexScopeWork work) {
		return new StubIndexScopeWorkAssert( work );
	}

	private final StubIndexScopeWork actual;

	private String messageBase = "Index-scope work did not match: ";

	private StubIndexScopeWorkAssert(StubIndexScopeWork actual) {
		this.actual = actual;
	}

	public StubIndexScopeWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubIndexScopeWorkAssert matches(StubIndexScopeWork expected) {
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );

		builder.startObject();

		boolean hasAnyMismatch;
		boolean mismatch = checkForMismatch( builder, "type", expected.getType(), actual.getType() );
		hasAnyMismatch = mismatch;
		mismatch = checkForMismatch( builder, "tenantIdentifier",
				expected.getTenantIdentifier(), actual.getTenantIdentifier()
		);
		hasAnyMismatch = hasAnyMismatch || mismatch;

		builder.endObject();

		if ( hasAnyMismatch ) {
			Assert.fail( messageBase + builder.toString() );
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
