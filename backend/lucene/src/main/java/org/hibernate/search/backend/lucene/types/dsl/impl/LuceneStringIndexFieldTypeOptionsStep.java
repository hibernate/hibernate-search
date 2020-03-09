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
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneTextFieldAggregationBuilderFactory;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStringFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneTextFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class LuceneStringIndexFieldTypeOptionsStep
		extends AbstractLuceneStandardIndexFieldTypeOptionsStep<LuceneStringIndexFieldTypeOptionsStep, String>
		implements StringIndexFieldTypeOptionsStep<LuceneStringIndexFieldTypeOptionsStep> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private String analyzerName;
	private Analyzer analyzer;

	private String searchAnalyzerName;
	private Analyzer searchAnalyzer;

	private String normalizerName;
	private Analyzer normalizer;

	private Norms norms = Norms.DEFAULT;
	private TermVector termVector = TermVector.DEFAULT;

	private Sortable sortable = Sortable.DEFAULT;

	LuceneStringIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		this.analyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, getBuildContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		this.searchAnalyzerName = searchAnalyzerName;
		this.searchAnalyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( searchAnalyzerName );
		if ( searchAnalyzer == null ) {
			throw log.unknownAnalyzer( searchAnalyzerName, getBuildContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		this.normalizer = getAnalysisDefinitionRegistry().getNormalizerDefinition( normalizerName );
		if ( normalizer == null ) {
			throw log.unknownNormalizer( normalizerName, getBuildContext().getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep norms(Norms norms) {
		this.norms = norms;
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep termVector(TermVector termVector) {
		this.termVector = termVector;
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<String> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );
		boolean resolvedNorms = resolveNorms();
		ResolvedTermVector resolvedTermVector = resolveTermVector();

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

			if ( resolvedAggregable ) {
				throw log.cannotUseAnalyzerOnAggregableField( analyzerName, getBuildContext().getEventContext() );
			}
		}
		else if ( searchAnalyzer != null ) {
			throw log.searchAnalyzerWithoutAnalyzer( searchAnalyzerName, getBuildContext().getEventContext() );
		}

		Analyzer analyzerOrNormalizer = analyzer != null ? analyzer : normalizer;
		if ( analyzerOrNormalizer == null ) {
			analyzerOrNormalizer = AnalyzerConstants.KEYWORD_ANALYZER;
		}

		DslConverter<?, ? extends String> dslConverter = createDslConverter();
		DslConverter<String, ? extends String> rawDslConverter = createRawDslConverter();
		ProjectionConverter<? super String, ?> projectionConverter = createProjectionConverter();
		ProjectionConverter<? super String, String> rawProjectionConverter = createRawProjectionConverter();
		LuceneStringFieldCodec codec = new LuceneStringFieldCodec(
				resolvedSearchable, resolvedSortable, resolvedAggregable,
				getFieldType( resolvedProjectable, resolvedSearchable, analyzer != null, resolvedNorms, resolvedTermVector ),
				indexNullAsValue,
				analyzerOrNormalizer
		);

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneTextFieldPredicateBuilderFactory<>(
						resolvedSearchable, dslConverter, rawDslConverter, codec,
						( searchAnalyzer != null ) ? searchAnalyzer : analyzerOrNormalizer
				),
				new LuceneTextFieldSortBuilderFactory<>(
						resolvedSortable, dslConverter, rawDslConverter, codec
				),
				new LuceneStandardFieldProjectionBuilderFactory<>(
						resolvedProjectable, projectionConverter, rawProjectionConverter, codec
				),
				new LuceneTextFieldAggregationBuilderFactory(
						resolvedAggregable,
						dslConverter, rawDslConverter,
						projectionConverter, rawProjectionConverter,
						codec,
						analyzer != null
				),
				resolvedAggregable,
				analyzerOrNormalizer
		);
	}

	@Override
	protected LuceneStringIndexFieldTypeOptionsStep thisAsS() {
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

	private ResolvedTermVector resolveTermVector() {
		switch ( termVector ) {
			// using NO as default to be consistent with Elasticsearch,
			// the default for Lucene would be WITH_POSITIONS_OFFSETS
			case NO:
			case DEFAULT:
				return new ResolvedTermVector( false, false, false, false );
			case YES:
				return new ResolvedTermVector( true, false, false, false );
			case WITH_POSITIONS:
				return new ResolvedTermVector( true, true, false, false );
			case WITH_OFFSETS:
				return new ResolvedTermVector( true, false, true, false );
			case WITH_POSITIONS_OFFSETS:
				return new ResolvedTermVector( true, true, true, false );
			case WITH_POSITIONS_PAYLOADS:
				return new ResolvedTermVector( true, true, false, true );
			case WITH_POSITIONS_OFFSETS_PAYLOADS:
				return new ResolvedTermVector( true, true, true, true );
			default:
				throw new AssertionFailure( "Unexpected value for TermVector: " + termVector );
		}
	}

	private static FieldType getFieldType(boolean projectable, boolean searchable, boolean analyzed, boolean norms, ResolvedTermVector termVector) {
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
			termVector.applyTo( fieldType );
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

	private static final class ResolvedTermVector {
		private final boolean store;
		private final boolean positions;
		private final boolean offsets;
		private final boolean payloads;

		private ResolvedTermVector(boolean store, boolean positions, boolean offsets, boolean payloads) {
			this.store = store;
			this.positions = positions;
			this.offsets = offsets;
			this.payloads = payloads;
		}

		private void applyTo( FieldType fieldType ) {
			fieldType.setStoreTermVectors( store );
			fieldType.setStoreTermVectorPositions( positions );
			fieldType.setStoreTermVectorOffsets( offsets );
			fieldType.setStoreTermVectorPayloads( payloads );
		}
	}
}
