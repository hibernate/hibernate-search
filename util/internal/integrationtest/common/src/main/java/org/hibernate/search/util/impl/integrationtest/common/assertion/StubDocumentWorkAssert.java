/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeDiffer;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubTreeNodeMismatch;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.StubDocumentNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.common.impl.ToStringStyle;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;

public class StubDocumentWorkAssert {

	public static final StubTreeNodeDiffer<StubDocumentNode> DEFAULT_DOCUMENT_DIFFER = StubTreeNodeDiffer.<StubDocumentNode>builder().build();

	public static StubDocumentWorkAssert assertThatDocumentWork(StubDocumentWork work) {
		return new StubDocumentWorkAssert( work );
	}

	private final StubDocumentWork actual;

	private StubTreeNodeDiffer<StubDocumentNode> documentDiffer = DEFAULT_DOCUMENT_DIFFER;

	private String messageBase = "Document work did not match: ";

	private StubDocumentWorkAssert(StubDocumentWork actual) {
		this.actual = actual;
	}

	public StubDocumentWorkAssert as(String messageBase) {
		this.messageBase = messageBase;
		return this;
	}

	public StubDocumentWorkAssert documentDiffer(StubTreeNodeDiffer<StubDocumentNode> differ) {
		this.documentDiffer = differ;
		return this;
	}

	public StubDocumentWorkAssert matches(StubDocumentWork expected) {
		ToStringTreeBuilder builder = new ToStringTreeBuilder( ToStringStyle.multilineDelimiterStructure() );

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
		mismatch = checkForMismatch( builder, "commitStrategy", expected.getCommitStrategy(), actual.getCommitStrategy() );
		hasAnyMismatch = hasAnyMismatch || mismatch;
		mismatch = checkForMismatch( builder, "refreshStrategy", expected.getRefreshStrategy(), actual.getRefreshStrategy() );
		hasAnyMismatch = hasAnyMismatch || mismatch;

		Map<String, StubTreeNodeMismatch> documentMismatches =
				documentDiffer.diff( expected.getDocument(), actual.getDocument() );
		if ( !documentMismatches.isEmpty() ) {
			hasAnyMismatch = true;
			builder.startObject( "document" );
			StubTreeNodeDiffer.appendTo( builder, documentMismatches );
			builder.endObject();
		}

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
