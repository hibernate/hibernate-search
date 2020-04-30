/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;


public class LuceneIndexModel implements AutoCloseable {

	private final String indexName;

	private final String mappedTypeName;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;

	private final LuceneIndexSchemaObjectNode rootNode;
	private final Map<String, LuceneIndexSchemaObjectNode> objectNodes;
	private final Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes;
	private final List<LuceneIndexSchemaObjectFieldTemplate> objectFieldTemplates;
	private final List<LuceneIndexSchemaFieldTemplate> fieldTemplates;
	private final ConcurrentMap<String, LuceneIndexSchemaObjectNode> dynamicObjectNodesCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, LuceneIndexSchemaFieldNode<?>> dynamicFieldNodesCache = new ConcurrentHashMap<>();

	private final ModelBasedScopedAnalyzer indexingAnalyzer;

	public LuceneIndexModel(String indexName,
			String mappedTypeName,
			ToDocumentIdentifierValueConverter<?> idDslConverter,
			LuceneIndexSchemaObjectNode rootNode,
			Map<String, LuceneIndexSchemaObjectNode> objectNodes,
			Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes,
			List<LuceneIndexSchemaObjectFieldTemplate> objectFieldTemplates,
			List<LuceneIndexSchemaFieldTemplate> fieldTemplates) {
		this.indexName = indexName;
		this.mappedTypeName = mappedTypeName;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.objectNodes = CollectionHelper.toImmutableMap( objectNodes );
		this.fieldNodes = CollectionHelper.toImmutableMap( fieldNodes );
		this.indexingAnalyzer = new ModelBasedScopedAnalyzer();
		this.objectFieldTemplates = objectFieldTemplates;
		this.fieldTemplates = fieldTemplates;
	}

	@Override
	public void close() {
		indexingAnalyzer.close();
	}

	public String getIndexName() {
		return indexName;
	}

	public String getMappedTypeName() {
		return mappedTypeName;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		return idDslConverter;
	}

	public LuceneIndexSchemaObjectNode getRootNode() {
		return rootNode;
	}

	public LuceneIndexSchemaObjectNode getObjectNode(String absolutePath, IndexFieldFilter filter) {
		LuceneIndexSchemaObjectNode node =
				getNode( objectNodes, objectFieldTemplates, dynamicObjectNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.getInclusion() );
	}

	public LuceneIndexSchemaFieldNode<?> getFieldNode(String absolutePath, IndexFieldFilter filter) {
		LuceneIndexSchemaFieldNode<?> node =
				getNode( fieldNodes, fieldTemplates, dynamicFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.getInclusion() );
	}

	public Analyzer getIndexingAnalyzer() {
		return indexingAnalyzer;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( "]" )
				.toString();
	}

	private <N> N getNode(Map<String, N> staticNodes,
			List<? extends AbstractLuceneIndexSchemaFieldTemplate<N>> templates,
			ConcurrentMap<String, N> dynamicNodesCache,
			String absolutePath) {
		N node = staticNodes.get( absolutePath );
		if ( node != null ) {
			return node;
		}
		node = dynamicNodesCache.get( absolutePath );
		if ( node != null ) {
			return node;
		}
		for ( AbstractLuceneIndexSchemaFieldTemplate<N> template : templates ) {
			node = template.createNodeIfMatching( this, absolutePath );
			if ( node != null ) {
				N previous = dynamicNodesCache.putIfAbsent( absolutePath, node );
				if ( previous != null ) {
					// Some other thread created the node before us.
					// Keep the first created node, discard ours: they are identical.
					node = previous;
				}
				break;
			}
		}
		return node;
	}

	/**
	 * An analyzer similar to {@link org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer},
	 * except the field &rarr; analyzer map is implemented by querying the model.
	 * This allows taking into account dynamic fields created through templates.
	 */
	private class ModelBasedScopedAnalyzer extends DelegatingAnalyzerWrapper {
		protected ModelBasedScopedAnalyzer() {
			super( PER_FIELD_REUSE_STRATEGY );
		}

		@Override
		protected Analyzer getWrappedAnalyzer(String fieldName) {
			LuceneIndexSchemaFieldNode<?> field = getFieldNode( fieldName, IndexFieldFilter.ALL );
			Analyzer analyzer = field.getType().getAnalyzerOrNormalizer();

			if ( analyzer == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			return analyzer;
		}
	}
}
