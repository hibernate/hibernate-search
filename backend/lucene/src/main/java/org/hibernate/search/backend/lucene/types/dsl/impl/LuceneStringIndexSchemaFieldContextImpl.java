/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.StringFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.StringFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.StringFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.StringFieldSortContributor;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.model.dsl.Store;
import org.hibernate.search.engine.backend.document.model.dsl.StringIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.QueryBuilder;

/**
 * @author Guillaume Smet
 */
public class LuceneStringIndexSchemaFieldContextImpl
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneStringIndexSchemaFieldContextImpl, String>
		implements StringIndexSchemaFieldTypedContext<LuceneStringIndexSchemaFieldContextImpl> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private Analyzer analyzer;
	private String normalizerName;
	private Analyzer normalizer;

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneStringIndexSchemaFieldContextImpl(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, String.class );
	}

	@Override
	public LuceneStringIndexSchemaFieldContextImpl analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		this.analyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, getSchemaContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexSchemaFieldContextImpl normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		this.normalizer = getAnalysisDefinitionRegistry().getNormalizerDefinition( normalizerName );
		if ( normalizer == null ) {
			throw log.unknownNormalizer( normalizerName, getSchemaContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexSchemaFieldContextImpl sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<String> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		if ( analyzer != null ) {
			switch ( sortable ) {
				case DEFAULT:
				case NO:
					break;
				case YES:
					throw log.cannotUseAnalyzerOnSortableField( analyzerName, getSchemaContext().getEventContext() );
			}

			if ( normalizer != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, getSchemaContext().getEventContext() );
			}
		}

		// TODO GSM: the idea would be to create only one global QueryBuilder object per analyzer/normalizer
		Analyzer analyzerOrNormalizer = analyzer != null ? analyzer : normalizer;
		QueryBuilder queryBuilder = analyzerOrNormalizer != null ? new QueryBuilder( analyzerOrNormalizer ) : null;

		StringFieldConverter converter = new StringFieldConverter(
				helper.createUserIndexFieldConverter(),
				analyzerOrNormalizer
		);

		LuceneIndexSchemaFieldNode<String> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				new StringFieldCodec(
						sortable,
						getFieldType( getStore(), analyzer != null ),
						analyzerOrNormalizer
				),
				new StringFieldPredicateBuilderFactory( converter, analyzer != null, queryBuilder ),
				StringFieldSortContributor.INSTANCE
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );

		if ( analyzerOrNormalizer != null ) {
			collector.collectAnalyzer( schemaNode.getAbsoluteFieldPath(), analyzerOrNormalizer );
		}
	}

	@Override
	protected LuceneStringIndexSchemaFieldContextImpl thisAsS() {
		return this;
	}

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return getSchemaContext().getRoot().getAnalysisDefinitionRegistry();
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
			/*
			 * Note that the name "tokenized" is misleading: it actually means "should the analyzer (or normalizer) be applied".
			 * When it's false, the analyzer/normalizer is completely ignored, not just tokenization.
			 * Thus it should be true even when just using a normalizer.
			 */
			fieldType.setTokenized( true );
		}
		switch ( store ) {
			case DEFAULT:
			case NO:
				fieldType.setStored( false );
				break;
			case YES:
				fieldType.setStored( true );
				break;
			case COMPRESS:
				// TODO HSEARCH-3081
				break;
		}
		fieldType.freeze();

		return fieldType;
	}
}
