/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexCompositeNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexCompositeNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

abstract class AbstractStubIndexCompositeNodeBuilder implements IndexCompositeNodeBuilder {

	protected final StubIndexSchemaDataNode.Builder schemaDataNodeBuilder;
	private final Map<String, StubIndexFieldBuilder> fieldBuilders = new LinkedHashMap<>();

	AbstractStubIndexCompositeNodeBuilder(StubIndexSchemaDataNode.Builder schemaDataNodeBuilder) {
		this.schemaDataNodeBuilder = schemaDataNodeBuilder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(String relativeFieldName,
			TreeNodeInclusion inclusion, IndexFieldType<F> indexFieldType) {
		StubIndexSchemaDataNode.Builder childSchemaNodeBuilder =
				StubIndexSchemaDataNode.field( schemaDataNodeBuilder, relativeFieldName );
		StubIndexValueFieldType<F> stubType = (StubIndexValueFieldType<F>) indexFieldType;
		stubType.apply( childSchemaNodeBuilder );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			getRootNodeBuilder().getBackendBehavior().onAddField(
					getRootNodeBuilder().getIndexName(),
					childSchemaNodeBuilder.getAbsolutePath()
			);
			schemaDataNodeBuilder.child( childSchemaNodeBuilder );
		}
		StubIndexValueFieldBuilder<F> childBuilder =
				new StubIndexValueFieldBuilder<>( childSchemaNodeBuilder, inclusion, stubType );
		fieldBuilders.put( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexObjectFieldBuilder addObjectField(String relativeFieldName, TreeNodeInclusion inclusion,
			ObjectStructure structure) {
		StubIndexSchemaDataNode.Builder childSchemaNodeBuilder =
				StubIndexSchemaDataNode.objectField( schemaDataNodeBuilder, relativeFieldName );
		StubIndexCompositeNodeType type = new StubIndexCompositeNodeType.Builder( structure ).build();
		type.apply( childSchemaNodeBuilder );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			getRootNodeBuilder().getBackendBehavior().onAddField(
					getRootNodeBuilder().getIndexName(),
					childSchemaNodeBuilder.getAbsolutePath()
			);
			schemaDataNodeBuilder.child( childSchemaNodeBuilder );
		}
		StubIndexObjectFieldBuilder childBuilder =
				new StubIndexObjectFieldBuilder( this, childSchemaNodeBuilder, inclusion, type );
		fieldBuilders.put( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String relativeNamedPredicateName,
			TreeNodeInclusion inclusion, PredicateDefinition definition) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.namedPredicate( schemaDataNodeBuilder, relativeNamedPredicateName )
						.predicateDefinition( definition );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexNamedPredicateBuilder( childBuilder );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			TreeNodeInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.fieldTemplate( schemaDataNodeBuilder, templateName );
		StubIndexValueFieldType<?> stubType = (StubIndexValueFieldType<?>) indexFieldType;
		stubType.apply( childBuilder );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexFieldTemplateNodeBuilder( childBuilder );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, TreeNodeInclusion inclusion) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.objectFieldTemplate( schemaDataNodeBuilder, templateName );
		StubIndexCompositeNodeType type = new StubIndexCompositeNodeType.Builder( structure ).build();
		type.apply( childBuilder );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexFieldTemplateNodeBuilder( childBuilder );
	}

	final void contributeChildren(StubIndexCompositeNode parent, Map<String, StubIndexField> staticChildren,
			BiConsumer<String, StubIndexField> fieldCollector) {
		for ( Map.Entry<String, StubIndexFieldBuilder> entry : fieldBuilders.entrySet() ) {
			StubIndexField field = entry.getValue().build( parent, fieldCollector );
			staticChildren.put( entry.getKey(), field );
			fieldCollector.accept( field.absolutePath(), field );
		}
	}

	abstract StubIndexRootBuilder getRootNodeBuilder();

}
