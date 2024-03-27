/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.fail;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeMismatch;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubProjectionNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

public class StubSearchWorkAssert {

	private static final String NEWLINE = "\n\t";

	private final StubTreeNodeDiffer<StubProjectionNode> PROJECTION_DIFFER =
			StubTreeNodeDiffer.<StubProjectionNode>builder().build();

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
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );

		builder.startObject();

		boolean hasAnyMismatch = false;
		boolean mismatch;

		if ( expected.getRootProjection() != null ) {
			Map<String, StubTreeNodeMismatch> mismatchesByPath =
					PROJECTION_DIFFER.diff( expected.getRootProjection(), actual.getRootProjection() );
			if ( !mismatchesByPath.isEmpty() ) {
				hasAnyMismatch = true;
				builder.startObject( "rootProjection" );
				StubTreeNodeDiffer.appendTo( builder, mismatchesByPath );
				builder.endObject();
			}
		}

		mismatch = checkForMismatch( builder, "routingKeys", expected.getRoutingKeys(), actual.getRoutingKeys() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "truncateAfterTimeout", expected.getTruncateAfterTimeout(),
				actual.getTruncateAfterTimeout() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "truncateAfterTimeUnit", expected.getTruncateAfterTimeUnit(),
				actual.getTruncateAfterTimeUnit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch =
				checkForMismatch( builder, "failAfterTimeout", expected.getFailAfterTimeout(), actual.getFailAfterTimeout() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "failAfterTimeUnit", expected.getFailAfterTimeUnit(),
				actual.getFailAfterTimeUnit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "offset", expected.getOffset(), actual.getOffset() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		mismatch = checkForMismatch( builder, "limit", expected.getLimit(), actual.getLimit() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		builder.endObject();

		if ( hasAnyMismatch ) {
			fail( messageBase + builder );
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
