/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

abstract class AbstractStubIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {

	protected final StubIndexSchemaNode.Builder builder;

	AbstractStubIndexSchemaObjectNodeBuilder(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public IndexSchemaFieldContext addField(String relativeFieldName) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( builder, relativeFieldName );
		getRootNodeBuilder().getBackendBehavior().onAddField(
				getRootNodeBuilder().getIndexName(),
				childBuilder.getAbsolutePath()
		);
		builder.child( childBuilder );
		return new StubIndexSchemaFieldContext( childBuilder, true );
	}

	@Override
	public IndexSchemaFieldContext createExcludedField(String relativeFieldName) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( builder, relativeFieldName );
		return new StubIndexSchemaFieldContext( childBuilder, false );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		getRootNodeBuilder().getBackendBehavior().onAddField(
				getRootNodeBuilder().getIndexName(),
				childBuilder.getAbsolutePath()
		);
		builder.child( childBuilder );
		return new StubIndexSchemaObjectFieldNodeBuilder( this, childBuilder, true );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName,
			ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		return new StubIndexSchemaObjectFieldNodeBuilder( this, childBuilder, false );
	}

	abstract StubIndexSchemaRootNodeBuilder getRootNodeBuilder();

}
