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

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeContributor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public abstract class AbstractElasticsearchIndexSchemaObjectNodeBuilder implements IndexSchemaObjectNodeBuilder {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, ElasticsearchIndexSchemaNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, ElasticsearchIndexSchemaNodeContributor> templates = new LinkedHashMap<>();

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
			String relativeFieldName, IndexFieldType<F> indexFieldType) {
		ElasticsearchIndexFieldType<F> elasticsearchIndexFieldType = (ElasticsearchIndexFieldType<F>) indexFieldType;
		ElasticsearchIndexSchemaFieldNodeBuilder<F> childBuilder = new ElasticsearchIndexSchemaFieldNodeBuilder<>(
				this, relativeFieldName, elasticsearchIndexFieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public <F> IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> createExcludedField(
			String relativeFieldName, IndexFieldType<F> indexFieldType) {
		ElasticsearchIndexFieldType<F> elasticsearchIndexFieldType = (ElasticsearchIndexFieldType<F>) indexFieldType;
		return new ElasticsearchIndexSchemaFieldNodeBuilder<>(
				this, relativeFieldName, elasticsearchIndexFieldType
		);
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		ElasticsearchIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new ElasticsearchIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder createExcludedObjectField(String relativeFieldName, ObjectFieldStorage storage) {
		return new ElasticsearchIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, storage );
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexFieldType<?> elasticsearchIndexFieldType = (ElasticsearchIndexFieldType<?>) indexFieldType;
		ElasticsearchIndexSchemaFieldTemplateBuilder templateBuilder = new ElasticsearchIndexSchemaFieldTemplateBuilder(
				this, prefixedTemplateName, elasticsearchIndexFieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> createExcludedFieldTemplate(String templateName,
			IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexFieldType<?> elasticsearchIndexFieldType = (ElasticsearchIndexFieldType<?>) indexFieldType;
		return new ElasticsearchIndexSchemaFieldTemplateBuilder(
				this, prefixedTemplateName, elasticsearchIndexFieldType, prefix
		);
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectFieldStorage storage, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		ElasticsearchIndexSchemaObjectFieldTemplateBuilder templateBuilder =
				new ElasticsearchIndexSchemaObjectFieldTemplateBuilder(
						this, prefixedTemplateName, storage, prefix
				);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> createExcludedObjectFieldTemplate(String templateName,
			ObjectFieldStorage storage, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		return new ElasticsearchIndexSchemaObjectFieldTemplateBuilder(
				this, prefixedTemplateName, storage, prefix
		);
	}

	final void contributeChildren(AbstractTypeMapping mapping, ElasticsearchIndexSchemaObjectNode node,
			ElasticsearchIndexSchemaNodeCollector collector) {
		for ( Map.Entry<String, ElasticsearchIndexSchemaNodeContributor> entry : fields.entrySet() ) {
			ElasticsearchIndexSchemaNodeContributor propertyContributor = entry.getValue();
			propertyContributor.contribute( collector, node, mapping );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( ElasticsearchIndexSchemaNodeContributor template : templates.values() ) {
			template.contribute( collector, node, mapping );
		}
	}

	abstract ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final DynamicType resolveSelfDynamicType() {
		if ( templates.isEmpty() ) {
			// TODO HSEARCH-3273 allow to configure this, both at index level (configuration properties) and at field level (ElasticsearchExtension)
			return DynamicType.STRICT;
		}
		else {
			return DynamicType.TRUE;
		}
	}

	private void putField(String name, ElasticsearchIndexSchemaNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, getEventContext() );
		}
	}

	private void putTemplate(String name, ElasticsearchIndexSchemaNodeContributor contributor) {
		Object previous = templates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaFieldTemplateNameConflict( name, getEventContext() );
		}
	}

}
