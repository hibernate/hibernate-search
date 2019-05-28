/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStringFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneTextFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @author Guillaume Smet
 */
class LuceneStringIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneStringIndexFieldTypeContext, String>
		implements StringIndexFieldTypeContext<LuceneStringIndexFieldTypeContext> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private Analyzer analyzer;
	private String normalizerName;
	private Analyzer normalizer;

	private Norms norms = Norms.DEFAULT;

	private Sortable sortable = Sortable.DEFAULT;

	LuceneStringIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public LuceneStringIndexFieldTypeContext analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		this.analyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, getBuildContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeContext normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		this.normalizer = getAnalysisDefinitionRegistry().getNormalizerDefinition( normalizerName );
		if ( normalizer == null ) {
			throw log.unknownNormalizer( normalizerName, getBuildContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeContext norms(Norms norms) {
		this.norms = norms;
		return thisAsS();
	}

	@Override
	public LuceneStringIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<String> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedNorms = resolveNorms();

		if ( analyzer != null ) {
			if ( resolvedSortable ) {
				throw log.cannotUseAnalyzerOnSortableField( analyzerName, getBuildContext().getEventContext() );
			}

			if ( normalizer != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, getBuildContext().getEventContext() );
			}

			if ( indexNullAsValue != null ) {
				throw log.cannotUseIndexNullAsAndAnalyzer( analyzerName, indexNullAsValue, getBuildContext().getEventContext() );
			}
		}

		Analyzer analyzerOrNormalizer = analyzer != null ? analyzer : normalizer;

		ToDocumentFieldValueConverter<?, ? extends String> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super String, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneStringFieldCodec codec = new LuceneStringFieldCodec(
				resolvedSearchable, resolvedSortable,
				getFieldType( resolvedProjectable, resolvedSearchable, analyzer != null, resolvedNorms ), indexNullAsValue,
				analyzerOrNormalizer
		);

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneTextFieldPredicateBuilderFactory<>( resolvedSearchable, dslToIndexConverter, createToDocumentRawConverter(), codec, analyzerOrNormalizer, analyzer, normalizer ),
				new LuceneTextFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec ),
				analyzerOrNormalizer
		);
	}

	@Override
	protected LuceneStringIndexFieldTypeContext thisAsS() {
		return this;
	}

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return getBuildContext().getAnalysisDefinitionRegistry();
	}

	private boolean resolveNorms() {
		switch ( norms ) {
			case YES:
				return true;
			case NO:
				return false;
			case DEFAULT:
				return ( analyzerName != null );
			default:
				throw new AssertionFailure( "Unexpected value for Norms: " + norms );
		}
	}

	private static FieldType getFieldType(boolean projectable, boolean searchable, boolean analyzed, boolean norms) {
		FieldType fieldType = new FieldType();

		if ( !searchable ) {
			fieldType.setIndexOptions( IndexOptions.NONE );
			fieldType.setStored( projectable );
			fieldType.freeze();
			return fieldType;
		}

		if ( analyzed ) {
			// TODO HSEARCH-3048 take into account term vectors option
			fieldType.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
			fieldType.setStoreTermVectors( true );
			fieldType.setStoreTermVectorPositions( true );
			fieldType.setStoreTermVectorOffsets( true );
			fieldType.setTokenized( true );
		}
		else {
			fieldType.setIndexOptions( IndexOptions.DOCS );
			/*
			 * Note that the name "tokenized" is misleading: it actually means "should the analyzer (or normalizer) be applied".
			 * When it's false, the analyzer/normalizer is completely ignored, not just tokenization.
			 * Thus it should be true even when just using a normalizer.
			 */
			fieldType.setTokenized( true );
		}

		fieldType.setStored( projectable );
		fieldType.setOmitNorms( !norms );
		fieldType.freeze();
		return fieldType;
	}
}
