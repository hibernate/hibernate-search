/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.fail;

import java.util.Map;

import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeMismatch;

public class StubTreeNodeAssert<T extends StubTreeNode<T>> {

	public static <T extends StubTreeNode<T>> StubTreeNodeAssert<T> assertThatTree(T node) {
		return new StubTreeNodeAssert<>( node );
	}

	private final T actual;

	private final StubTreeNodeDiffer<T> differ = StubTreeNodeDiffer.<T>builder().build();

	private String messageBase = "StubTreeNode did not match: ";

	private StubTreeNodeAssert(T actual) {
		this.actual = actual;
	}

	public StubTreeNodeAssert<T> as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubTreeNodeAssert<T> matches(T expected) {
		Map<String, StubTreeNodeMismatch> mismatchesByPath = differ.diff( expected, actual );
		if ( !mismatchesByPath.isEmpty() ) {
			ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );
			builder.startObject();
			StubTreeNodeDiffer.appendTo( builder, mismatchesByPath );
			builder.endObject();
			fail( messageBase + builder.toString() );
		}
		return this;
	}
}
