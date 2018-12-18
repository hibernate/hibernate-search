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
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.engine.backend.document.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.util.EventContext;

public class LuceneIndexSchemaRootNodeBuilder extends AbstractLuceneIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder, LuceneIndexSchemaRootContext {

	private final String indexName;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private ToDocumentIdentifierValueConverter<?> idDslConverter;

	public LuceneIndexSchemaRootNodeBuilder(String indexName,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.indexName = indexName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	@Override
	public EventContext getEventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public void explicitRouting() {
		// TODO GSM support explicit routing?
		throw new UnsupportedOperationException( "explicitRouting not supported right now" );
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idDslConverter) {
		this.idDslConverter = idDslConverter;
	}

	@Override
	public LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return analysisDefinitionRegistry;
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRoot() {
		return this;
	}

	public LuceneIndexModel build(String indexName) {
		Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder = new HashMap<>();
		Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder = new HashMap<>();
		// TODO the default analyzer should be configurable, for now, we default to no analysis
		ScopedAnalyzer.Builder scopedAnalyzerBuilder = new ScopedAnalyzer.Builder( new KeywordAnalyzer() );

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

	public EventContext getIndexEventContext() {
		return EventContexts.fromIndexName( indexName );
	}
}
