/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.rule;

import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

import static org.hibernate.search.integrationtest.util.common.assertion.StubTreeNodeAssert.assertThat;

class PushSchemaCall {

	private final String indexName;
	private final StubIndexSchemaNode schemaNode;

	PushSchemaCall(String indexName, StubIndexSchemaNode schemaNode) {
		this.indexName = indexName;
		this.schemaNode = schemaNode;
	}

	public Void verify(PushSchemaCall actualCall) {
		assertThat( actualCall.schemaNode )
				.as( "Schema for index '" + indexName + "' did not match:" )
				.matches( schemaNode );
		return null;
	}

	@Override
	public String toString() {
		return "push schema to index '" + indexName + "'; schema = " + schemaNode;
	}

}
