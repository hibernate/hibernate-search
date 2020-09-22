/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;


public class LuceneIndexModel implements AutoCloseable, IndexDescriptor {

	private final String indexName;

	private final String mappedTypeName;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;

	private final LuceneIndexSchemaObjectNode rootNode;
	private final Map<String, LuceneIndexSchemaObjectFieldNode> objectFieldNodes;
	private final Map<String, LuceneIndexSchemaValueFieldNode<?>> valueFieldNodes;
	private final List<IndexFieldDescriptor> staticFields;
	private final List<LuceneIndexSchemaObjectFieldTemplate> objectFieldTemplates;
	private final List<LuceneIndexSchemaValueFieldTemplate> valueFieldTemplates;
	private final boolean hasNestedDocuments;
	private final ConcurrentMap<String, LuceneIndexSchemaObjectFieldNode> dynamicObjectFieldNodesCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, LuceneIndexSchemaValueFieldNode<?>> dynamicValueFieldNodesCache = new ConcurrentHashMap<>();

	private final IndexingScopedAnalyzer indexingAnalyzer;
	private final SearchScopedAnalyzer searchAnalyzer;

	public LuceneIndexModel(String indexName,
			String mappedTypeName,
			ToDocumentIdentifierValueConverter<?> idDslConverter,
			LuceneIndexSchemaObjectNode rootNode,
			Map<String, LuceneIndexSchemaObjectFieldNode> objectFieldNodes,
			Map<String, LuceneIndexSchemaValueFieldNode<?>> valueFieldNodes,
			List<LuceneIndexSchemaObjectFieldTemplate> objectFieldTemplates,
			List<LuceneIndexSchemaValueFieldTemplate> valueFieldTemplates) {
		this.indexName = indexName;
		this.mappedTypeName = mappedTypeName;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.objectFieldNodes = CollectionHelper.toImmutableMap( objectFieldNodes );
		this.valueFieldNodes = CollectionHelper.toImmutableMap( valueFieldNodes );
		List<IndexFieldDescriptor> theStaticFields = new ArrayList<>();
		objectFieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.forEach( theStaticFields::add );
		valueFieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.forEach( theStaticFields::add );
		this.staticFields = CollectionHelper.toImmutableList( theStaticFields );
		this.indexingAnalyzer = new IndexingScopedAnalyzer();
		this.searchAnalyzer = new SearchScopedAnalyzer();
		this.objectFieldTemplates = objectFieldTemplates;
		this.valueFieldTemplates = valueFieldTemplates;
		this.hasNestedDocuments = initHasNestedDocuments();
	}

	@Override
	public void close() {
		indexingAnalyzer.close();
	}

	@Override
	public String hibernateSearchName() {
		return indexName;
	}

	@Override
	public LuceneIndexSchemaObjectNode root() {
		return rootNode;
	}

	@Override
	public Optional<IndexFieldDescriptor> field(String absolutePath) {
		IndexFieldDescriptor fieldDescriptor = getFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		if ( fieldDescriptor == null ) {
			fieldDescriptor = getObjectFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		}
		return Optional.ofNullable( fieldDescriptor );
	}

	@Override
	public Collection<IndexFieldDescriptor> staticFields() {
		return staticFields;
	}

	public String mappedTypeName() {
		return mappedTypeName;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}

	public ToDocumentIdentifierValueConverter<?> idDslConverter() {
		return idDslConverter;
	}

	public LuceneIndexSchemaObjectFieldNode getObjectFieldNode(String absolutePath, IndexFieldFilter filter) {
		LuceneIndexSchemaObjectFieldNode node =
				getNode( objectFieldNodes, objectFieldTemplates, dynamicObjectFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.inclusion() );
	}

	public LuceneIndexSchemaValueFieldNode<?> getFieldNode(String absolutePath, IndexFieldFilter filter) {
		LuceneIndexSchemaValueFieldNode<?> node =
				getNode( valueFieldNodes, valueFieldTemplates, dynamicValueFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.inclusion() );
	}

	public boolean hasNestedDocuments() {
		return hasNestedDocuments;
	}

	public Analyzer getIndexingAnalyzer() {
		return indexingAnalyzer;
	}

	public Analyzer getSearchAnalyzer() {
		return searchAnalyzer;
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
	 * except the field &rarr; analyzer map is implemented by querying the model
	 * and retrieving the indexing analyzer.
	 * This allows taking into account dynamic fields created through templates.
	 */
	private class IndexingScopedAnalyzer extends DelegatingAnalyzerWrapper {
		protected IndexingScopedAnalyzer() {
			super( PER_FIELD_REUSE_STRATEGY );
		}

		@Override
		protected Analyzer getWrappedAnalyzer(String fieldName) {
			LuceneIndexSchemaValueFieldNode<?> field = getFieldNode( fieldName, IndexFieldFilter.ALL );
			if ( field == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			Analyzer analyzer = field.type().indexingAnalyzerOrNormalizer();
			if ( analyzer == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			return analyzer;
		}
	}
	/**
	 * An analyzer similar to {@link org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer},
	 * except the field &rarr; analyzer map is implemented by querying the model
	 * and retrieving the search analyzer.
	 * This allows taking into account dynamic fields created through templates.
	 */
	private class SearchScopedAnalyzer extends DelegatingAnalyzerWrapper {
		protected SearchScopedAnalyzer() {
			super( PER_FIELD_REUSE_STRATEGY );
		}

		@Override
		protected Analyzer getWrappedAnalyzer(String fieldName) {
			LuceneIndexSchemaValueFieldNode<?> field = getFieldNode( fieldName, IndexFieldFilter.ALL );
			if ( field == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			Analyzer analyzer = field.type().searchAnalyzerOrNormalizer();
			if ( analyzer == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			return analyzer;
		}
	}

	private boolean initHasNestedDocuments() {
		for ( LuceneIndexSchemaObjectFieldNode node : objectFieldNodes.values() ) {
			if ( isNested( node.structure() ) ) {
				return true;
			}
		}
		for ( LuceneIndexSchemaObjectFieldTemplate template : objectFieldTemplates ) {
			if ( isNested( template.structure() ) ) {
				return true;
			}
		}
		return false;
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
