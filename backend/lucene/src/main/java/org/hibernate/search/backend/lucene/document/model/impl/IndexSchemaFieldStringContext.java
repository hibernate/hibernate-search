/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;

/**
 * @author Guillaume Smet
 */
class IndexSchemaFieldStringContext extends AbstractLuceneIndexSchemaFieldTypedContext<String> {

	private static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer();

	private Sortable sortable;

	private Analyzer analyzer;

	private Analyzer normalizer;

	public IndexSchemaFieldStringContext(String fieldName) {
		super( fieldName );
	}

	@Override
	public IndexSchemaFieldStringContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldAccessor<String> accessor, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		// TODO For now, we allow the sortable - analyzed combination, it will be disallowed later
//		if ( Sortable.YES.equals( getSortable() ) && getAnalyzer() != null ) {
//			throw log.cannotUseAnalyzerOnSortableField( getFieldName() );
//		}

		// TODO GSM: the idea would be to create only one global QueryBuilder object per analyzer/normalizer
		Analyzer analyzerOrNormalizer = analyzer != null ? analyzer : normalizer;
		QueryBuilder queryBuilder = analyzerOrNormalizer != null ? new QueryBuilder( analyzerOrNormalizer ) : null;

		LuceneIndexSchemaFieldNode<String> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getFieldName(),
				new StringFieldFormatter(
						sortable,
						getFieldType( getStore(), analyzer != null ),
						analyzerOrNormalizer
				),
				new StringFieldQueryBuilder( analyzerOrNormalizer, analyzer != null, queryBuilder )
		);

		accessor.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );

		if ( analyzerOrNormalizer != null ) {
			collector.collectAnalyzer( schemaNode.getAbsoluteFieldPath(), analyzerOrNormalizer );
		}
	}

	@Override
	public IndexSchemaFieldTypedContext<String> analyzer(String analyzerName) {
		if ( !"default".equals( analyzerName ) ) {
			throw new UnsupportedOperationException( "For now, only the default analyzer is supported by the Lucene backend." );
		}
		this.analyzer = STANDARD_ANALYZER;
		return this;
	}

	@Override
	public IndexSchemaFieldTypedContext<String> normalizer(String normalizerName) {
		throw new UnsupportedOperationException( "For now, normalizers are not supported by the Lucene backend." );
	}

	@Override
	protected Analyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	protected Analyzer getNormalizer() {
		return normalizer;
	}

	private static FieldType getFieldType(Store store, boolean analyzed) {
		FieldType fieldType = new FieldType();
		if ( analyzed ) {
			// TODO GSM: take into account the norms and term vectors options
			fieldType.setOmitNorms( false );
			fieldType.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
			fieldType.setStoreTermVectors( true );
			fieldType.setStoreTermVectorPositions( true );
			fieldType.setStoreTermVectorOffsets( true );
			fieldType.setTokenized( true );
		}
		else {
			fieldType.setOmitNorms( true );
			fieldType.setIndexOptions( IndexOptions.DOCS );
			fieldType.setTokenized( false );
		}
		fieldType.setStored( Store.YES.equals( store ) );
		fieldType.freeze();

		return fieldType;
	}

	private static final class StringFieldFormatter implements LuceneFieldFormatter<String> {

		private final Sortable sortable;

		private final FieldType fieldType;

		private final Analyzer normalizer;

		private final int hashCode;

		private StringFieldFormatter(Sortable sortable, FieldType fieldType, Analyzer normalizer) {
			this.sortable = sortable;
			this.fieldType = fieldType;
			this.normalizer = normalizer;

			this.hashCode = buildHashCode();
		}

		@Override
		public void addFields(LuceneDocumentBuilder documentBuilder, LuceneIndexSchemaObjectNode parentNode, String fieldName, String value) {
			if ( value == null ) {
				return;
			}

			documentBuilder.addField( parentNode, new Field( fieldName, value, fieldType ) );

			if ( Sortable.YES.equals( sortable ) ) {
				documentBuilder.addField( parentNode, new SortedDocValuesField(
						fieldName,
						new BytesRef( normalizer != null ? AnalyzerUtils.analyzeSortableValue( normalizer, fieldName, value ) : value )
				) );
			}
		}

		@Override
		public Object format(Object value) {
			return value;
		}

		@Override
		public Type getDefaultSortFieldType() {
			return SortField.Type.STRING;
		}

		@Override
		public Object getSortMissingFirst() {
			return SortField.STRING_FIRST;
		}

		@Override
		public Object getSortMissingLast() {
			return SortField.STRING_LAST;
		}

		@Override
		public String parse(Document document, String fieldName) {
			return document.get( fieldName );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( StringFieldFormatter.class != obj.getClass() ) {
				return false;
			}

			StringFieldFormatter other = (StringFieldFormatter) obj;

			return Objects.equals( sortable, other.sortable ) &&
					Objects.equals( fieldType, other.fieldType ) &&
					Objects.equals( normalizer, other.normalizer );
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		private int buildHashCode() {
			return Objects.hash( sortable, fieldType, normalizer );
		}
	}

	private static class StringFieldQueryBuilder implements LuceneFieldQueryFactory {

		private final Analyzer analyzerOrNormalizer;

		private final boolean tokenized;

		private final QueryBuilder queryBuilder;

		private final int hashCode;

		private StringFieldQueryBuilder(Analyzer analyzerOrNormalizer, boolean tokenized, QueryBuilder queryBuilder) {
			this.analyzerOrNormalizer = analyzerOrNormalizer;
			this.tokenized = tokenized;
			this.queryBuilder = queryBuilder;

			this.hashCode = buildHashCode();
		}

		@Override
		public Query createMatchQuery(String fieldName, Object value, MatchQueryOptions matchQueryOptions) {
			String stringValue = (String) value;

			if ( queryBuilder != null ) {
				return queryBuilder.createBooleanQuery( fieldName, stringValue, matchQueryOptions.getOperator() );
			}
			else {
				// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
				// the queryBuilder case above

				return new TermQuery( new Term( fieldName, getAnalyzedValue( analyzerOrNormalizer, fieldName, stringValue ) ) );
			}
		}

		@Override
		public Query createRangeQuery(String fieldName, Object lowerLimit, Object upperLimit, RangeQueryOptions rangeQueryOptions) {
			// Note that a range query only makes sense if only one token is returned by the analyzer
			// and we should even consider forcing having a normalizer here, instead of supporting
			// range queries on analyzed fields.

			return TermRangeQuery.newStringRange(
					fieldName,
					getAnalyzedValue( analyzerOrNormalizer, fieldName, (String) lowerLimit ),
					getAnalyzedValue( analyzerOrNormalizer, fieldName, (String) upperLimit ),
					// we force the true value if the limit is null because of some Lucene checks down the hill
					lowerLimit == null ? true : !rangeQueryOptions.isExcludeLowerLimit(),
					upperLimit == null ? true : !rangeQueryOptions.isExcludeUpperLimit()
			);
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( StringFieldFormatter.class != obj.getClass() ) {
				return false;
			}

			StringFieldQueryBuilder other = (StringFieldQueryBuilder) obj;

			return Objects.equals( analyzerOrNormalizer, other.analyzerOrNormalizer ) &&
					tokenized == other.tokenized &&
					Objects.equals( queryBuilder, other.queryBuilder );
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		private int buildHashCode() {
			return Objects.hash( analyzerOrNormalizer, tokenized, queryBuilder );
		}

		private static String getAnalyzedValue(Analyzer analyzerOrNormalizer, String fieldName, String stringValue) {
			if ( analyzerOrNormalizer == null ) {
				return stringValue;
			}

			return AnalyzerUtils.analyzeSortableValue( analyzerOrNormalizer, fieldName, stringValue );
		}
	}
}
