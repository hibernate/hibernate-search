/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateFactory;

public abstract class AbstractElasticsearchIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, ElasticsearchIndexSchemaNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, ElasticsearchIndexSchemaNodeContributor> templates = new LinkedHashMap<>();
	private final Map<String, ElasticsearchIndexSchemaNodeContributor> namedPredicates = new LinkedHashMap<>();

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
			String relativeFieldName, IndexFieldInclusion inclusion, IndexFieldType<F> indexFieldType) {
		ElasticsearchIndexValueFieldType<F> fieldType = (ElasticsearchIndexValueFieldType<F>) indexFieldType;
		ElasticsearchIndexSchemaValueFieldNodeBuilder<F> childBuilder = new ElasticsearchIndexSchemaValueFieldNodeBuilder<>(
				this, relativeFieldName, inclusion, fieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectStructure structure) {
		ElasticsearchIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new ElasticsearchIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, inclusion, structure );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String relativeNamedPredicateName,
			IndexFieldInclusion inclusion, NamedPredicateFactory factory) {
		ElasticsearchIndexSchemaNamedPredicateFactoryBuilder childBuilder = new ElasticsearchIndexSchemaNamedPredicateFactoryBuilder(
			this, relativeNamedPredicateName, factory
		);
		putNamedPredicate( relativeNamedPredicateName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexValueFieldType<?> fieldType = (ElasticsearchIndexValueFieldType<?>) indexFieldType;
		ElasticsearchIndexSchemaValueFieldTemplateBuilder templateBuilder = new ElasticsearchIndexSchemaValueFieldTemplateBuilder(
				this, prefixedTemplateName, inclusion, fieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, IndexFieldInclusion inclusion) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexSchemaObjectFieldTemplateBuilder templateBuilder =
				new ElasticsearchIndexSchemaObjectFieldTemplateBuilder(
						this, prefixedTemplateName, inclusion, structure, prefix
				);
		if ( IndexFieldInclusion.INCLUDED.equals( inclusion ) ) {
			putTemplate( prefixedTemplateName, templateBuilder );
		}
		return templateBuilder;
	}

	final void contributeChildren(AbstractTypeMapping mapping, ElasticsearchIndexSchemaObjectNode node,
			ElasticsearchIndexSchemaNodeCollector collector,
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByNameForParent) {
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor> entry : fields.entrySet() ) {
			ElasticsearchIndexSchemaNodeContributor propertyContributor = entry.getValue();
			propertyContributor.contribute( collector, node, staticChildrenByNameForParent, mapping );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( ElasticsearchIndexSchemaNodeContributor template : templates.values() ) {
			template.contribute( collector, node, staticChildrenByNameForParent, mapping );
		}
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor> entry : namedPredicates.entrySet() ) {
			ElasticsearchIndexSchemaNodeContributor propertyContributor = entry.getValue();
			propertyContributor.contribute( collector, node, staticChildrenByNameForParent, mapping );
		}
	}

	abstract ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final DynamicType resolveSelfDynamicType(DynamicType defaultDynamicType) {
		return ( templates.isEmpty() ) ? defaultDynamicType : DynamicType.TRUE;
	}

	private void putField(String name, ElasticsearchIndexSchemaNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, eventContext() );
		}
	}

	private void putTemplate(String name, ElasticsearchIndexSchemaNodeContributor contributor) {
		Object previous = templates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaFieldTemplateNameConflict( name, eventContext() );
		}
	}

	private void putNamedPredicate(String name, ElasticsearchIndexSchemaNodeContributor contributor) {
		Object previous = namedPredicates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNamedPredicateNameConflict( name, eventContext() );
		}
	}
}
