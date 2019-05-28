/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactoryContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIndexFieldTypeFactoryContextImpl;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaBuildContext;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIndexSchemaRootNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder, IndexSchemaBuildContext {

	private final EventContext indexEventContext;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private ToDocumentIdentifierValueConverter<?> idDslConverter;

	public LuceneIndexSchemaRootNodeBuilder(EventContext indexEventContext,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.indexEventContext = indexEventContext;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public EventContext getEventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public LuceneIndexFieldTypeFactoryContext createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return new LuceneIndexFieldTypeFactoryContextImpl( indexEventContext, analysisDefinitionRegistry, defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		// TODO HSEARCH-3314 support explicit routing (for static sharding in particular)
		throw new UnsupportedOperationException( "explicitRouting not supported right now" );
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idDslConverter) {
		this.idDslConverter = idDslConverter;
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return this;
	}

	public LuceneIndexModel build(String indexName) {
		Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder = new HashMap<>();
		Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder = new HashMap<>();
		ScopedAnalyzer.Builder scopedAnalyzerBuilder = new ScopedAnalyzer.Builder();

		LuceneIndexSchemaNodeCollector collector = new LuceneIndexSchemaNodeCollector() {
			@Override
			public void collectAnalyzer(String absoluteFieldPath, Analyzer analyzer) {
				scopedAnalyzerBuilder.setAnalyzer( absoluteFieldPath, analyzer );
			}

			@Override
			public void collectFieldNode(String absoluteFieldPath, LuceneIndexSchemaFieldNode<?> node) {
				fieldNodesBuilder.put( absoluteFieldPath, node );
			}

			@Override
			public void collectObjectNode(String absolutePath, LuceneIndexSchemaObjectNode node) {
				objectNodesBuilder.put( absolutePath, node );
			}
		};

		LuceneIndexSchemaObjectNode rootNode = LuceneIndexSchemaObjectNode.root();
		contributeChildren( rootNode, collector );

		return new LuceneIndexModel(
				indexName,
				idDslConverter == null ? new StringToDocumentIdentifierValueConverter() : idDslConverter,
				objectNodesBuilder,
				fieldNodesBuilder,
				scopedAnalyzerBuilder.build()
		);
	}

	@Override
	String getAbsolutePath() {
		return null;
	}

	EventContext getIndexEventContext() {
		return indexEventContext;
	}
}
