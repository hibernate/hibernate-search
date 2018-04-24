/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.StringFieldCodec;
import org.hibernate.search.backend.lucene.types.formatter.impl.StringFieldFormatter;
import org.hibernate.search.backend.lucene.types.predicate.impl.StringFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.StringFieldSortContributor;

/**
 * @author Guillaume Smet
 */
public class StringIndexSchemaFieldContext extends AbstractLuceneIndexSchemaFieldTypedContext<String> {

	private static final Analyzer STANDARD_ANALYZER = new StandardAnalyzer();

	private Sortable sortable;

	private Analyzer analyzer;

	private Analyzer normalizer;

	public StringIndexSchemaFieldContext(String fieldName) {
		super( fieldName );
	}

	@Override
	public StringIndexSchemaFieldContext sortable(Sortable sortable) {
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

		StringFieldFormatter formatter = new StringFieldFormatter( analyzerOrNormalizer );

		LuceneIndexSchemaFieldNode<String> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getFieldName(),
				formatter,
				new StringFieldCodec(
						sortable,
						getFieldType( getStore(), analyzer != null ),
						analyzerOrNormalizer
				),
				new StringFieldPredicateBuilderFactory( formatter, analyzer != null, queryBuilder ),
				StringFieldSortContributor.INSTANCE
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
}
