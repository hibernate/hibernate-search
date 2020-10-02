/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubTreeNodeAssert.assertThatTree;

import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class SchemaDefinitionCall extends Call<SchemaDefinitionCall> {

	private final String indexName;
	private final StubIndexSchemaNode schemaNode;
	private final Consumer<StubIndexSchemaNode> capture;

	SchemaDefinitionCall(String indexName, StubIndexSchemaNode schemaNode) {
		this.indexName = indexName;
		this.schemaNode = schemaNode;
		this.capture = null;
	}

	SchemaDefinitionCall(String indexName, StubIndexSchemaNode schemaNode, Consumer<StubIndexSchemaNode> capture) {
		this.indexName = indexName;
		this.schemaNode = schemaNode;
		this.capture = capture;
	}

	public CallBehavior<Void> verify(SchemaDefinitionCall actualCall) {
		if ( schemaNode != null ) {
			assertThatTree( actualCall.schemaNode )
					.as( "Schema for index '" + indexName + "' did not match:\n" )
					.matches( schemaNode );
			return () -> {
				capture.accept( actualCall.schemaNode );
				return null;
			};
		}
		else {
			// We don't care about the actual schema
			return () -> null;
		}
	}

	@Override
	protected boolean isSimilarTo(SchemaDefinitionCall other) {
		return Objects.equals( indexName, other.indexName );
	}

	@Override
	public String toString() {
		return "schema definition for index '" + indexName + "'; schema = " + schemaNode;
	}

}
