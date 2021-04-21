/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.AbstractLuceneIndexSchemaFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaRootNode;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactory;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIndexFieldTypeFactoryImpl;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIndexSchemaRootNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder, IndexSchemaBuildContext {

	private final EventContext indexEventContext;
	private final String mappedTypeName;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private DocumentIdentifierValueConverter<?> idDslConverter;

	public LuceneIndexSchemaRootNodeBuilder(EventContext indexEventContext,
			String mappedTypeName, LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.indexEventContext = indexEventContext;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public LuceneIndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return new LuceneIndexFieldTypeFactoryImpl( indexEventContext, analysisDefinitionRegistry, defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		// Nothing to do
	}

	@Override
	public void idDslConverter(DocumentIdentifierValueConverter<?> idDslConverter) {
		this.idDslConverter = idDslConverter;
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return this;
	}

	public LuceneIndexModel build(String indexName) {
		Map<String, AbstractLuceneIndexSchemaFieldNode> staticFields = new HashMap<>();
		List<AbstractLuceneIndexSchemaFieldTemplate<?>> fieldTemplates = new ArrayList<>();
		// Initializing a one-element array so that we can mutate the boolean below.
		// Alternatively we could use AtomicBoolean, but we don't need concurrent access here.
		boolean[] hasNestedDocument = new boolean[1];

		LuceneIndexSchemaNodeCollector collector = new LuceneIndexSchemaNodeCollector() {
			@Override
			public void collect(String absoluteFieldPath, LuceneIndexSchemaValueFieldNode<?> node) {
				staticFields.put( absoluteFieldPath, node );
			}

			@Override
			public void collect(String absolutePath, LuceneIndexSchemaObjectFieldNode node) {
				staticFields.put( absolutePath, node );
				if ( isNested( node.structure() ) ) {
					hasNestedDocument[0] = true;
				}
			}

			@Override
			public void collect(LuceneIndexSchemaObjectFieldTemplate template) {
				fieldTemplates.add( template );
				if ( isNested( template.structure() ) ) {
					hasNestedDocument[0] = true;
				}
			}

			@Override
			public void collect(LuceneIndexSchemaValueFieldTemplate template) {
				fieldTemplates.add( template );
			}
		};

		Map<String, AbstractLuceneIndexSchemaFieldNode> staticChildrenByName = new TreeMap<>();
		LuceneIndexSchemaRootNode rootNode = new LuceneIndexSchemaRootNode( staticChildrenByName, buildQueryElementFactoryMap() );
		contributeChildren( rootNode, collector, staticChildrenByName );

		return new LuceneIndexModel(
				indexName,
				mappedTypeName,
				idDslConverter == null ? new StringDocumentIdentifierValueConverter() : idDslConverter,
				rootNode, staticFields, fieldTemplates, hasNestedDocument[0]
		);
	}

	@Override
	String getAbsolutePath() {
		return null;
	}

	EventContext getIndexEventContext() {
		return indexEventContext;
	}

	private boolean isNested(ObjectStructure structure) {
		switch ( structure ) {
			case NESTED:
				return true;
			case FLATTENED:
			case DEFAULT:
			default:
				return false;
		}
	}
}
