/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexModel;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;

public class LuceneIndexModel extends AbstractIndexModel<LuceneIndexModel, LuceneIndexRoot, LuceneIndexField>
		implements AutoCloseable, IndexDescriptor {

	private final boolean hasNestedDocuments;

	private final IndexingScopedAnalyzer indexingAnalyzer;
	private final SearchScopedAnalyzer searchAnalyzer;

	public LuceneIndexModel(String hibernateSearchName, String mappedTypeName,
			IndexIdentifier identifier,
			LuceneIndexRoot rootNode, Map<String, LuceneIndexField> staticFields,
			List<? extends AbstractLuceneIndexFieldTemplate<?>> fieldTemplates,
			boolean hasNestedDocuments) {
		super( hibernateSearchName, mappedTypeName, identifier, rootNode, staticFields, fieldTemplates );
		this.indexingAnalyzer = new IndexingScopedAnalyzer();
		this.searchAnalyzer = new SearchScopedAnalyzer();
		this.hasNestedDocuments = hasNestedDocuments;
	}

	@Override
	public void close() {
		indexingAnalyzer.close();
	}

	@Override
	protected LuceneIndexModel self() {
		return this;
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
			LuceneIndexField field = fieldOrNull( fieldName, IndexFieldFilter.ALL );
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
			LuceneIndexField field = fieldOrNull( fieldName, IndexFieldFilter.ALL );
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
