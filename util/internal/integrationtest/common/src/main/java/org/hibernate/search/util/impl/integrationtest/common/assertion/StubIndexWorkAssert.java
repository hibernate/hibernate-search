/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeCompare;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeMismatch;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.util.impl.common.ToStringStyle;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

import org.junit.Assert;

public class StubIndexWorkAssert {

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
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.MULTILINE );

		builder.startObject();

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
			builder.startObject( "document" );
			StubTreeNodeCompare.appendTo( builder, documentMismatches );
			builder.endObject();
		}

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
