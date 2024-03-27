/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.fail;

import java.util.Objects;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;

public class StubSchemaManagementWorkAssert {

	public static StubSchemaManagementWorkAssert assertThatSchemaManagementWork(StubSchemaManagementWork work) {
		return new StubSchemaManagementWorkAssert( work );
	}

	private final StubSchemaManagementWork actual;

	private String messageBase = "Schema management work did not match: ";

	private StubSchemaManagementWorkAssert(StubSchemaManagementWork actual) {
		this.actual = actual;
	}

	public StubSchemaManagementWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubSchemaManagementWorkAssert matches(StubSchemaManagementWork expected) {
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );

		builder.startObject();

		boolean hasAnyMismatch = checkForMismatch( builder, "type", expected.getType(), actual.getType() );

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
