/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;


public class LuceneIndexModel implements AutoCloseable, IndexDescriptor {

	private final String indexName;

	private final String mappedTypeName;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;

	private final LuceneIndexSchemaObjectNode rootNode;
	private final Map<String, AbstractLuceneIndexSchemaFieldNode> staticFields;
	private final List<IndexFieldDescriptor> includedStaticFields;
	private final List<AbstractLuceneIndexSchemaFieldTemplate<?>> fieldTemplates;
	private final boolean hasNestedDocuments;
	private final ConcurrentMap<String, AbstractLuceneIndexSchemaFieldNode> dynamicFieldsCache = new ConcurrentHashMap<>();

	private final IndexingScopedAnalyzer indexingAnalyzer;
	private final SearchScopedAnalyzer searchAnalyzer;

	public LuceneIndexModel(String indexName,
			String mappedTypeName,
			ToDocumentIdentifierValueConverter<?> idDslConverter,
			LuceneIndexSchemaObjectNode rootNode,
			Map<String, AbstractLuceneIndexSchemaFieldNode> staticFields,
			List<AbstractLuceneIndexSchemaFieldTemplate<?>> fieldTemplates,
			boolean hasNestedDocuments) {
		this.indexName = indexName;
		this.mappedTypeName = mappedTypeName;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.staticFields = CollectionHelper.toImmutableMap( staticFields );
		this.includedStaticFields = CollectionHelper.toImmutableList( staticFields.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.collect( Collectors.toList() ) );
		this.indexingAnalyzer = new IndexingScopedAnalyzer();
		this.searchAnalyzer = new SearchScopedAnalyzer();
		this.fieldTemplates = fieldTemplates;
		this.hasNestedDocuments = hasNestedDocuments;
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
		return Optional.ofNullable( fieldOrNull( absolutePath ) );
	}

	public AbstractLuceneIndexSchemaFieldNode fieldOrNull(String absolutePath) {
		return fieldOrNull( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
	}

	public AbstractLuceneIndexSchemaFieldNode fieldOrNull(String absolutePath, IndexFieldFilter filter) {
		AbstractLuceneIndexSchemaFieldNode field = fieldOrNullIgnoringInclusion( absolutePath );
		return field == null ? null : filter.filter( field, field.inclusion() );
	}

	@Override
	public Collection<IndexFieldDescriptor> staticFields() {
		return includedStaticFields;
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

	private AbstractLuceneIndexSchemaFieldNode fieldOrNullIgnoringInclusion(String absolutePath) {
		AbstractLuceneIndexSchemaFieldNode field = staticFields.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		field = dynamicFieldsCache.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		for ( AbstractLuceneIndexSchemaFieldTemplate<?> template : fieldTemplates ) {
			field = template.createNodeIfMatching( this, absolutePath );
			if ( field != null ) {
				AbstractLuceneIndexSchemaFieldNode previous = dynamicFieldsCache.putIfAbsent( absolutePath, field );
				if ( previous != null ) {
					// Some other thread created the node before us.
					// Keep the first created node, discard ours: they are identical.
					field = previous;
				}
				break;
			}
		}
		return field;
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
			AbstractLuceneIndexSchemaFieldNode field = fieldOrNull( fieldName, IndexFieldFilter.ALL );
			if ( field == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			Analyzer analyzer = field.toValueField().type().indexingAnalyzerOrNormalizer();
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
			AbstractLuceneIndexSchemaFieldNode field = fieldOrNull( fieldName, IndexFieldFilter.ALL );
			if ( field == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			Analyzer analyzer = field.toValueField().type().searchAnalyzerOrNormalizer();
			if ( analyzer == null ) {
				return AnalyzerConstants.KEYWORD_ANALYZER;
			}

			return analyzer;
		}
	}
}
