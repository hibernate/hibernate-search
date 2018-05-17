/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubTreeNodeAssert;

class PushSchemaCall {

	private final String indexName;
	private final StubIndexSchemaNode schemaNode;

	PushSchemaCall(String indexName, StubIndexSchemaNode schemaNode) {
		this.indexName = indexName;
		this.schemaNode = schemaNode;
	}

	public Void verify(PushSchemaCall actualCall) {
		if ( schemaNode != null ) {
			StubTreeNodeAssert.assertThat( actualCall.schemaNode )
					.as( "Schema for index '" + indexName + "' did not match:\n" )
					.matches( schemaNode );
		}
		return null;
	}

	@Override
	public String toString() {
		return "push schema to index '" + indexName + "'; schema = " + schemaNode;
	}

}
