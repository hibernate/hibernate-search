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

class StubIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {

	protected final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaObjectNodeBuilder(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public IndexSchemaFieldContext addField(String relativeFieldName) {
		return new StubIndexSchemaFieldContext( builder, relativeFieldName, true );
	}

	@Override
	public IndexSchemaFieldContext createExcludedField(String relativeFieldName) {
		return new StubIndexSchemaFieldContext( builder, relativeFieldName, false );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		builder.child( childBuilder );
		return new StubIndexSchemaObjectFieldNodeBuilder( childBuilder, true );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName,
			ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		return new StubIndexSchemaObjectFieldNodeBuilder( childBuilder, false );
	}

}
