/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexCompositeNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.logging.impl.MappingLog;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchNamedPredicate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexCompositeNodeType;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
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
import org.hibernate.search.engine.search.predicate.definition.TypedPredicateDefinition;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public abstract class AbstractElasticsearchIndexCompositeNodeBuilder implements IndexCompositeNodeBuilder {

	protected final ElasticsearchIndexCompositeNodeType.Builder typeBuilder;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, ElasticsearchIndexNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, ElasticsearchIndexNodeContributor> templates = new LinkedHashMap<>();
	private final Set<String> namedPredicates = new LinkedHashSet<>();

	protected AbstractElasticsearchIndexCompositeNodeBuilder(
			ElasticsearchIndexCompositeNodeType.Builder typeBuilder) {
		this.typeBuilder = typeBuilder;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absolutePath=" ).append( getAbsolutePath() )
				.append( "]" )
				.toString();
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> addField(
			String relativeFieldName, TreeNodeInclusion inclusion, IndexFieldType<F> indexFieldType) {
		ElasticsearchIndexValueFieldType<F> fieldType = (ElasticsearchIndexValueFieldType<F>) indexFieldType;
		ElasticsearchIndexValueFieldBuilder<F> childBuilder = new ElasticsearchIndexValueFieldBuilder<>(
				this, relativeFieldName, inclusion, fieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexObjectFieldBuilder addObjectField(String relativeFieldName, TreeNodeInclusion inclusion,
			ObjectStructure structure) {
		ElasticsearchIndexObjectFieldBuilder objectFieldBuilder =
				new ElasticsearchIndexObjectFieldBuilder( this, relativeFieldName, inclusion, structure );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String name, TreeNodeInclusion inclusion,
			PredicateDefinition definition) {
		putNamedPredicate( name );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			typeBuilder.queryElementFactory( PredicateTypeKeys.named( name ),
					new ElasticsearchNamedPredicate.Factory( definition, name ) );
		}
		return new ElasticsearchIndexNamedPredicateOptions<>( inclusion, definition );
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String name,
			TreeNodeInclusion inclusion, TypedPredicateDefinition<?> definition) {
		putNamedPredicate( name );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			typeBuilder.queryElementFactory( PredicateTypeKeys.named( name ),
					new ElasticsearchNamedPredicate.TypedFactory<>( definition, name ) );
		}
		return new ElasticsearchIndexNamedPredicateOptions<>( inclusion, definition );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			TreeNodeInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexValueFieldType<?> fieldType = (ElasticsearchIndexValueFieldType<?>) indexFieldType;
		ElasticsearchIndexValueFieldTemplateBuilder templateBuilder = new ElasticsearchIndexValueFieldTemplateBuilder(
				this, prefixedTemplateName, inclusion, fieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, TreeNodeInclusion inclusion) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexObjectFieldTemplateBuilder templateBuilder =
				new ElasticsearchIndexObjectFieldTemplateBuilder(
						this, prefixedTemplateName, inclusion, structure, prefix
				);
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			putTemplate( prefixedTemplateName, templateBuilder );
		}
		return templateBuilder;
	}

	final void contributeChildren(AbstractTypeMapping mapping, ElasticsearchIndexCompositeNode node,
			ElasticsearchIndexNodeCollector collector,
			Map<String, ElasticsearchIndexField> staticChildrenByNameForParent) {
		for ( Map.Entry<String, ElasticsearchIndexNodeContributor> entry : fields.entrySet() ) {
			ElasticsearchIndexNodeContributor propertyContributor = entry.getValue();
			propertyContributor.contribute( collector, node, staticChildrenByNameForParent, mapping );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( ElasticsearchIndexNodeContributor template : templates.values() ) {
			template.contribute( collector, node, staticChildrenByNameForParent, mapping );
		}
	}

	abstract ElasticsearchIndexRootBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final DynamicType resolveSelfDynamicType(DynamicType defaultDynamicType) {
		return ( templates.isEmpty() ) ? defaultDynamicType : DynamicType.TRUE;
	}

	private void putField(String name, ElasticsearchIndexNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw MappingLog.INSTANCE.indexSchemaNodeNameConflict( name, eventContext() );
		}
	}

	private void putTemplate(String name, ElasticsearchIndexNodeContributor contributor) {
		Object previous = templates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw MappingLog.INSTANCE.indexSchemaFieldTemplateNameConflict( name, eventContext() );
		}
	}

	private void putNamedPredicate(String name) {
		if ( !namedPredicates.add( name ) ) {
			throw MappingLog.INSTANCE.indexSchemaNamedPredicateNameConflict( name, eventContext() );
		}
	}
}
