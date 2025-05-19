/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexCompositeNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexField;
import org.hibernate.search.backend.lucene.logging.impl.MappingLog;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneNamedPredicate;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexCompositeNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexObjectFieldBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.TypedPredicateDefinition;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

abstract class AbstractLuceneIndexCompositeNodeBuilder
		implements IndexCompositeNodeBuilder, IndexSchemaBuildContext {

	protected final LuceneIndexCompositeNodeType.Builder typeBuilder;

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, LuceneIndexNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, LuceneIndexNodeContributor> templates = new LinkedHashMap<>();
	private final Set<String> namedPredicates = new LinkedHashSet<>();

	protected AbstractLuceneIndexCompositeNodeBuilder(LuceneIndexCompositeNodeType.Builder typeBuilder) {
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
		LuceneIndexValueFieldType<F> luceneIndexFieldType = (LuceneIndexValueFieldType<F>) indexFieldType;
		LuceneIndexValueFieldBuilder<F> childBuilder = new LuceneIndexValueFieldBuilder<>(
				this, relativeFieldName, inclusion, luceneIndexFieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexObjectFieldBuilder addObjectField(String relativeFieldName, TreeNodeInclusion inclusion,
			ObjectStructure structure) {
		LuceneIndexObjectFieldBuilder objectFieldBuilder =
				new LuceneIndexObjectFieldBuilder( this, relativeFieldName, inclusion, structure );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String name,
			TreeNodeInclusion inclusion, TypedPredicateDefinition<?> definition) {
		putNamedPredicate( name );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			typeBuilder.queryElementFactory( PredicateTypeKeys.named( name ),
					new LuceneNamedPredicate.TypedFactory<>( definition, name ) );
		}
		return new LuceneIndexNamedPredicateOptions<>( inclusion, definition );
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String name, TreeNodeInclusion inclusion,
			PredicateDefinition definition) {
		putNamedPredicate( name );
		if ( TreeNodeInclusion.INCLUDED.equals( inclusion ) ) {
			typeBuilder.queryElementFactory( PredicateTypeKeys.named( name ),
					new LuceneNamedPredicate.Factory( definition, name ) );
		}
		return new LuceneIndexNamedPredicateOptions<>( inclusion, definition );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			TreeNodeInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexValueFieldType<?> elasticsearchIndexFieldType = (LuceneIndexValueFieldType<?>) indexFieldType;
		LuceneIndexValueFieldTemplateBuilder templateBuilder = new LuceneIndexValueFieldTemplateBuilder(
				this, prefixedTemplateName, inclusion, elasticsearchIndexFieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, TreeNodeInclusion inclusion) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexObjectFieldTemplateBuilder templateBuilder =
				new LuceneIndexObjectFieldTemplateBuilder(
						this, prefixedTemplateName, inclusion, structure, prefix
				);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	public abstract LuceneIndexRootBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final void contributeChildren(LuceneIndexCompositeNode node, LuceneIndexNodeCollector collector,
			Map<String, LuceneIndexField> staticChildrenByNameForParent) {
		for ( LuceneIndexNodeContributor contributor : fields.values() ) {
			contributor.contribute( collector, node, staticChildrenByNameForParent );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( LuceneIndexNodeContributor template : templates.values() ) {
			template.contribute( collector, node, staticChildrenByNameForParent );
		}
	}

	private void putField(String name, LuceneIndexNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw MappingLog.INSTANCE.indexSchemaNodeNameConflict( name, eventContext() );
		}
	}

	private void putTemplate(String name, LuceneIndexNodeContributor contributor) {
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
