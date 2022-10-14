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

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;

class SchemaDefinitionCall extends Call<SchemaDefinitionCall> {

	private final String indexName;
	private final StubIndexSchemaDataNode schemaNode;
	private final StubIndexModel model;
	private final Consumer<StubIndexModel> capture;

	SchemaDefinitionCall(String indexName, StubIndexModel model) {
		this.indexName = indexName;
		this.schemaNode = model.root().schemaData();
		this.model = model;
		this.capture = null;
	}

	SchemaDefinitionCall(String indexName, StubIndexSchemaDataNode schemaNode, Consumer<StubIndexModel> capture) {
		this.indexName = indexName;
		this.schemaNode = schemaNode;
		this.model = null;
		this.capture = capture;
	}

	public CallBehavior<Void> verify(SchemaDefinitionCall actualCall) {
		if ( schemaNode != null ) {
			assertThatTree( actualCall.schemaNode )
					.as( "Schema for index '" + indexName + "' did not match:\n" )
					.matches( schemaNode );
			return () -> {
				capture.accept( actualCall.model );
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
	protected String summary() {
		return "schema definition for index '" + indexName + "'; schema = " + schemaNode;
	}

}
