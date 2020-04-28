/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
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

	private final Map<String, LuceneIndexSchemaObjectNode> objectNodes;

	private final Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes;

	private final ModelBasedScopedAnalyzer indexingAnalyzer;

	public LuceneIndexModel(String indexName,
			String mappedTypeName,
			ToDocumentIdentifierValueConverter<?> idDslConverter,
			Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder,
			Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder) {
		this.indexName = indexName;
		this.mappedTypeName = mappedTypeName;
		this.idDslConverter = idDslConverter;
		this.fieldNodes = CollectionHelper.toImmutableMap( fieldNodesBuilder );
		this.objectNodes = CollectionHelper.toImmutableMap( objectNodesBuilder );
		this.indexingAnalyzer = new ModelBasedScopedAnalyzer();
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

	public LuceneIndexSchemaFieldNode<?> getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	public LuceneIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
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
			LuceneIndexSchemaFieldNode<?> field = getFieldNode( fieldName );
			Analyzer analyzer = field.getType().getAnalyzerOrNormalizer();

			if ( analyzer == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			return analyzer;
		}
	}
}
