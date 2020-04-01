/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFilterOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexFieldType;

abstract class AbstractStubIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {

	protected final StubIndexSchemaNode.Builder builder;

	AbstractStubIndexSchemaObjectNodeBuilder(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(String relativeFieldName,
		IndexFieldType<F> indexFieldType) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( builder, relativeFieldName );
		getRootNodeBuilder().getBackendBehavior().onAddField(
			getRootNodeBuilder().getIndexName(),
			childBuilder.getAbsolutePath()
		);
		StubIndexFieldType<F> stubIndexFieldType = (StubIndexFieldType<F>) indexFieldType;
		stubIndexFieldType.addField( childBuilder );
		builder.child( childBuilder );
		return new StubIndexSchemaFieldNodeBuilder<>( childBuilder, true );
	}

	@Override
	public <F extends FilterFactory> IndexSchemaFilterOptionsStep<?, IndexFilterReference<F>> addFilter(String relativeFilterName, F factory) {

		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.filter( builder, relativeFilterName )
			.filterFactory( factory );
		getRootNodeBuilder().getBackendBehavior().onAddField(
			getRootNodeBuilder().getIndexName(),
			childBuilder.getAbsolutePath()
		);

		builder.child( childBuilder );
		return new StubIndexSchemaFilterBuilder<>( childBuilder, true );
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> createExcludedField(String relativeFieldName,
		IndexFieldType<F> indexFieldType) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( builder, relativeFieldName );
		return new StubIndexSchemaFieldNodeBuilder<>( childBuilder, false );
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder
			= StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
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
		StubIndexSchemaNode.Builder childBuilder
			= StubIndexSchemaNode.objectField( builder, relativeFieldName, storage );
		return new StubIndexSchemaObjectFieldNodeBuilder( this, childBuilder, false );
	}

	abstract StubIndexSchemaRootNodeBuilder getRootNodeBuilder();

}
