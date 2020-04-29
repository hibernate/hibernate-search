/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectNodeBuilder, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, LuceneIndexSchemaNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, LuceneIndexSchemaNodeContributor> templates = new LinkedHashMap<>();

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
		LuceneIndexFieldType<F> luceneIndexFieldType = (LuceneIndexFieldType<F>) indexFieldType;
		LuceneIndexSchemaFieldNodeBuilder<F> childBuilder = new LuceneIndexSchemaFieldNodeBuilder<>(
				this, relativeFieldName, inclusion, luceneIndexFieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectFieldStorage storage) {
		LuceneIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, inclusion, storage );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexFieldType<?> elasticsearchIndexFieldType = (LuceneIndexFieldType<?>) indexFieldType;
		LuceneIndexSchemaFieldTemplateBuilder templateBuilder = new LuceneIndexSchemaFieldTemplateBuilder(
				this, prefixedTemplateName, inclusion, elasticsearchIndexFieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectFieldStorage storage, String prefix, IndexFieldInclusion inclusion) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexSchemaObjectFieldTemplateBuilder templateBuilder =
				new LuceneIndexSchemaObjectFieldTemplateBuilder(
						this, prefixedTemplateName, inclusion, storage, prefix
				);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	public abstract LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final void contributeChildren(LuceneIndexSchemaObjectNode node, LuceneIndexSchemaNodeCollector collector) {
		for ( LuceneIndexSchemaNodeContributor contributor : fields.values() ) {
			contributor.contribute( collector, node );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( LuceneIndexSchemaNodeContributor template : templates.values() ) {
			template.contribute( collector, node );
		}
	}

	final List<String> getChildrenNames() {
		// The Map#keySet() method for LinkedHashMap will return the set in insertion order
		return new ArrayList<>( fields.keySet() );
	}

	private void putField(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, getEventContext() );
		}
	}

	private void putTemplate(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = templates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaFieldTemplateNameConflict( name, getEventContext() );
		}
	}
}
