/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTemplateOptionsStep;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectNodeBuilder;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaNamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.factories.NamedPredicateProvider;

abstract class AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectNodeBuilder, IndexSchemaBuildContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	// Use a LinkedHashMap for deterministic iteration
	private final Map<String, LuceneIndexSchemaNodeContributor> fields = new LinkedHashMap<>();
	private final Map<String, LuceneIndexSchemaNodeContributor> templates = new LinkedHashMap<>();
	private final Map<String, LuceneIndexSchemaNodeContributor> namedPredicates = new LinkedHashMap<>();

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
		LuceneIndexValueFieldType<F> luceneIndexFieldType = (LuceneIndexValueFieldType<F>) indexFieldType;
		LuceneIndexSchemaValueFieldNodeBuilder<F> childBuilder = new LuceneIndexSchemaValueFieldNodeBuilder<>(
				this, relativeFieldName, inclusion, luceneIndexFieldType
		);
		putField( relativeFieldName, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaObjectFieldNodeBuilder addObjectField(String relativeFieldName, IndexFieldInclusion inclusion,
			ObjectStructure structure) {
		LuceneIndexSchemaObjectFieldNodeBuilder objectFieldBuilder =
				new LuceneIndexSchemaObjectFieldNodeBuilder( this, relativeFieldName, inclusion, structure );
		putField( relativeFieldName, objectFieldBuilder );
		return objectFieldBuilder;
	}

	@Override
	public IndexSchemaNamedPredicateOptionsStep addNamedPredicate(String name,
			IndexFieldInclusion inclusion, NamedPredicateProvider provider) {
		LuceneIndexSchemaNamedPredicateNodeBuilder childBuilder = new LuceneIndexSchemaNamedPredicateNodeBuilder(
				this, name, inclusion, provider
		);
		putNamedPredicate( name, childBuilder );
		return childBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addFieldTemplate(String templateName,
			IndexFieldInclusion inclusion, IndexFieldType<?> indexFieldType, String prefix) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexValueFieldType<?> elasticsearchIndexFieldType = (LuceneIndexValueFieldType<?>) indexFieldType;
		LuceneIndexSchemaValueFieldTemplateBuilder templateBuilder = new LuceneIndexSchemaValueFieldTemplateBuilder(
				this, prefixedTemplateName, inclusion, elasticsearchIndexFieldType, prefix
		);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	@Override
	public IndexSchemaFieldTemplateOptionsStep<?> addObjectFieldTemplate(String templateName,
			ObjectStructure structure, String prefix, IndexFieldInclusion inclusion) {
		String prefixedTemplateName = FieldPaths.prefix( prefix, templateName );
		LuceneIndexSchemaObjectFieldTemplateBuilder templateBuilder =
				new LuceneIndexSchemaObjectFieldTemplateBuilder(
						this, prefixedTemplateName, inclusion, structure, prefix
				);
		putTemplate( prefixedTemplateName, templateBuilder );
		return templateBuilder;
	}

	public abstract LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder();

	abstract String getAbsolutePath();

	final void contributeChildren(LuceneIndexSchemaObjectNode node, LuceneIndexSchemaNodeCollector collector,
			Map<String, AbstractLuceneIndexSchemaFieldNode> staticChildrenByNameForParent) {
		for ( LuceneIndexSchemaNodeContributor contributor : fields.values() ) {
			contributor.contribute( collector, node, staticChildrenByNameForParent );
		}
		// Contribute templates depth-first, so do ours after the children's.
		// The reason is templates defined in children have more precise path globs and thus
		// should be appear first in the list.
		for ( LuceneIndexSchemaNodeContributor template : templates.values() ) {
			template.contribute( collector, node, staticChildrenByNameForParent );
		}
		for ( LuceneIndexSchemaNodeContributor contributor : namedPredicates.values() ) {
			contributor.contribute( collector, node, staticChildrenByNameForParent );
		}
	}

	private void putField(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = fields.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNodeNameConflict( name, eventContext() );
		}
	}

	private void putTemplate(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = templates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaFieldTemplateNameConflict( name, eventContext() );
		}
	}

	private void putNamedPredicate(String name, LuceneIndexSchemaNodeContributor contributor) {
		Object previous = namedPredicates.putIfAbsent( name, contributor );
		if ( previous != null ) {
			throw log.indexSchemaNamedPredicateNameConflict( name, eventContext() );
		}
	}
}
