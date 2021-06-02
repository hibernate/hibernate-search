/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

abstract class AbstractStubIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {

	protected final StubIndexSchemaDataNode.Builder schemaDataNodeBuilder;
	private final Map<String, StubIndexSchemaFieldBuilder> fieldBuilders = new LinkedHashMap<>();

	AbstractStubIndexSchemaObjectNodeBuilder(StubIndexSchemaDataNode.Builder schemaDataNodeBuilder) {
		this.schemaDataNodeBuilder = schemaDataNodeBuilder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(String relativeFieldName,
			IndexFieldInclusion inclusion, IndexFieldType<F> indexFieldType) {
		StubIndexSchemaDataNode.Builder childSchemaNodeBuilder = StubIndexSchemaDataNode.field( schemaDataNodeBuilder, relativeFieldName );
		StubIndexValueFieldType<F> stubType = (StubIndexValueFieldType<F>) indexFieldType;
		stubType.apply( childSchemaNodeBuilder );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			getRootNodeBuilder().getBackendBehavior().onAddField(
					getRootNodeBuilder().getIndexName(),
					childSchemaNodeBuilder.getAbsolutePath()
			);
			schemaDataNodeBuilder.child( childSchemaNodeBuilder );
		}
		StubIndexSchemaValueFieldNodeBuilder<F> childBuilder =
				new StubIndexSchemaValueFieldNodeBuilder<>( childSchemaNodeBuilder, inclusion, stubType );
		fieldBuilders.put( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectStructure structure) {
		StubIndexSchemaDataNode.Builder childSchemaNodeBuilder =
				StubIndexSchemaDataNode.objectField( schemaDataNodeBuilder, relativeFieldName, structure );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			getRootNodeBuilder().getBackendBehavior().onAddField(
					getRootNodeBuilder().getIndexName(),
					childSchemaNodeBuilder.getAbsolutePath()
			);
			schemaDataNodeBuilder.child( childSchemaNodeBuilder );
		}
		StubIndexSchemaObjectFieldNodeBuilder childBuilder =
				new StubIndexSchemaObjectFieldNodeBuilder( this, childSchemaNodeBuilder, inclusion, structure );
		fieldBuilders.put( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String relativeNamedPredicateName,
		IndexFieldInclusion inclusion, NamedPredicateProvider provider) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.namedPredicate( schemaDataNodeBuilder, relativeNamedPredicateName )
						.namedPredicateProvider( provider );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexSchemaNamedPredicateBuilder( childBuilder );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.fieldTemplate( schemaDataNodeBuilder, templateName );
		StubIndexValueFieldType<?> stubType = (StubIndexValueFieldType<?>) indexFieldType;
		stubType.apply( childBuilder );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexSchemaFieldTemplateNodeBuilder( childBuilder );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, IndexFieldInclusion inclusion) {
		StubIndexSchemaDataNode.Builder childBuilder =
				StubIndexSchemaDataNode.objectFieldTemplate( schemaDataNodeBuilder, templateName, structure );
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			schemaDataNodeBuilder.child( childBuilder );
		}
		return new StubIndexSchemaFieldTemplateNodeBuilder( childBuilder );
	}

	final void contributeChildren(BiConsumer<String, StubIndexNode> consumer) {
		for ( StubIndexSchemaFieldBuilder fieldBuilder : fieldBuilders.values() ) {
			StubIndexNode field = fieldBuilder.build( consumer );
			consumer.accept( field.schemaData().absolutePath(), field );
		}
	}

	abstract StubIndexSchemaRootNodeBuilder getRootNodeBuilder();

}
