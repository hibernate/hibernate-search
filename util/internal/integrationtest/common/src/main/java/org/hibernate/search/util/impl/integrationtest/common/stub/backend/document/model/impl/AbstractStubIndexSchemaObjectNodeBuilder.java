/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexFieldType;

abstract class AbstractStubIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {

	protected final StubIndexSchemaNode.Builder builder;

	AbstractStubIndexSchemaObjectNodeBuilder(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(String relativeFieldName,
			IndexFieldInclusion inclusion, IndexFieldType<F> indexFieldType) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( builder, relativeFieldName );
		StubIndexFieldType<F> stubIndexFieldType = (StubIndexFieldType<F>) indexFieldType;
		stubIndexFieldType.addField( childBuilder );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			getRootNodeBuilder().getBackendBehavior().onAddField(
					getRootNodeBuilder().getIndexName(),
					childBuilder.getAbsolutePath()
			);
			builder.child( childBuilder );
		}
		return new StubIndexSchemaFieldNodeBuilder<>( childBuilder, inclusion );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		getRootNodeBuilder().getBackendBehavior().onAddField(
				getRootNodeBuilder().getIndexName(),
				childBuilder.getAbsolutePath()
		);
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			builder.child( childBuilder );
		}
		return new StubIndexSchemaObjectFieldNodeBuilder( this, childBuilder, inclusion );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.fieldTemplate( builder, templateName );
		StubIndexFieldType<?> stubIndexFieldType = (StubIndexFieldType<?>) indexFieldType;
		stubIndexFieldType.addField( childBuilder );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			builder.child( childBuilder );
		}
		return new StubIndexSchemaFieldTemplateNodeBuilder( childBuilder );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectFieldStorage storage, String prefix, IndexFieldInclusion inclusion) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.objectFieldTemplate( builder, templateName, storage );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			builder.child( childBuilder );
		}
		return new StubIndexSchemaFieldTemplateNodeBuilder( childBuilder );
	}

	abstract StubIndexSchemaRootNodeBuilder getRootNodeBuilder();

}
