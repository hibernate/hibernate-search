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
import org.apache.lucene.facet.FacetsConfig;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.backend.lucene.types.dsl.LuceneIndexFieldTypeFactory;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIndexFieldTypeFactoryImpl;
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
	private final String mappedTypeName;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private ToDocumentIdentifierValueConverter<?> idDslConverter;

	public LuceneIndexSchemaRootNodeBuilder(EventContext indexEventContext,
			String mappedTypeName, LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.indexEventContext = indexEventContext;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public EventContext getEventContext() {
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
		FacetsConfig facetsConfig = new FacetsConfig();

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

			@Override
			public void collectFacetConfig(String absoluteFieldPath, boolean multiValued) {
				facetsConfig.setMultiValued( absoluteFieldPath, multiValued );
			}
		};

		LuceneIndexSchemaObjectNode rootNode = LuceneIndexSchemaObjectNode.root();
		contributeChildren( rootNode, collector );

		return new LuceneIndexModel(
				indexName,
				mappedTypeName,
				idDslConverter == null ? new StringToDocumentIdentifierValueConverter() : idDslConverter,
				objectNodesBuilder,
				fieldNodesBuilder,
				scopedAnalyzerBuilder.build(),
				facetsConfig.getDimConfigs().isEmpty() ? null : facetsConfig
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
