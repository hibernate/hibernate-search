/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeCompare;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeMismatch;
import org.hibernate.search.util.impl.common.ToStringStyle;
import org.hibernate.search.util.impl.common.ToStringTreeBuilder;

import org.junit.Assert;

public class StubTreeNodeAssert<T extends StubTreeNode<T>> {

	public static <T extends StubTreeNode<T>> StubTreeNodeAssert<T> assertThat(T node) {
		return new StubTreeNodeAssert<>( node );
	}

	private final T actual;

	private String messageBase = "StubTreeNode did not match: ";

	private StubTreeNodeAssert(T actual) {
		this.actual = actual;
	}

	public StubTreeNodeAssert<T> as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubTreeNodeAssert<T> matches(T expected) {
		Map<String, StubTreeNodeMismatch> mismatchesByPath = StubTreeNodeCompare.compare( expected, actual );
		if ( !mismatchesByPath.isEmpty() ) {
			ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.MULTILINE );
			builder.startObject();
			StubTreeNodeCompare.appendTo( builder, mismatchesByPath );
			builder.endObject();
			Assert.fail( messageBase + builder.toString() );
		}
		return this;
	}
}
