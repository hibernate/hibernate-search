/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.AnalyzerConstants;
import org.hibernate.search.backend.lucene.search.predicate.impl.LucenePredicateTypeKeys;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldHighlightProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjection;
import org.hibernate.search.backend.lucene.types.aggregation.impl.LuceneTextTermsAggregation;
import org.hibernate.search.backend.lucene.types.codec.impl.DocValues;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneStringFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneCommonQueryStringPredicateBuilderFieldState;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneExistsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextMatchPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextPhrasePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextRangePredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextRegexpPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextTermsPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneTextWildcardPredicate;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneStandardFieldSort;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

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

	private Set<Highlightable> highlightable;

	LuceneStringIndexFieldTypeOptionsStep(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, String.class );
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep analyzer(String analyzerName) {
		this.analyzerName = analyzerName;
		this.analyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, buildContext.getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep searchAnalyzer(String searchAnalyzerName) {
		this.searchAnalyzerName = searchAnalyzerName;
		this.searchAnalyzer = getAnalysisDefinitionRegistry().getAnalyzerDefinition( searchAnalyzerName );
		if ( searchAnalyzer == null ) {
			throw log.unknownAnalyzer( searchAnalyzerName, buildContext.getEventContext() );
		}
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep normalizer(String normalizerName) {
		this.normalizerName = normalizerName;
		this.normalizer = getAnalysisDefinitionRegistry().getNormalizerDefinition( normalizerName );
		if ( normalizer == null ) {
			throw log.unknownNormalizer( normalizerName, buildContext.getEventContext() );
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
	public LuceneStringIndexFieldTypeOptionsStep highlightable(Collection<Highlightable> highlightable) {
		Contracts.assertNotNull( highlightable, "highlightable" );
		this.highlightable = highlightable.isEmpty() ? Collections.emptySet() : EnumSet.copyOf( highlightable );
		return this;
	}

	@Override
	public LuceneStringIndexFieldTypeOptionsStep sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexValueFieldType<String> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveProjectable();
		boolean resolvedSearchable = resolveDefault( searchable );
		boolean resolvedAggregable = resolveDefault( aggregable );
		boolean resolvedNorms = resolveNorms();
		ResolvedTermVector resolvedTermVector = resolveTermVector();
		builder.hasTermVectorsConfigured( resolvedTermVector.hasAnyEnabled() );

		DocValues docValues = resolvedSortable || resolvedAggregable ? DocValues.ENABLED : DocValues.DISABLED;

		if ( analyzer != null ) {
			builder.analyzerName( analyzerName );
			builder.searchAnalyzerName( searchAnalyzerName );
			builder.indexingAnalyzerOrNormalizer( analyzer );
			builder.searchAnalyzerOrNormalizer( searchAnalyzer != null ? searchAnalyzer : analyzer );

			Set<SearchHighlighterType> allowedHighlighterTypes = resolveAllowedHighlighterTypes();
			builder.allowedHighlighterTypes( allowedHighlighterTypes );
			if ( !allowedHighlighterTypes.isEmpty() ) {
				builder.queryElementFactory(
						ProjectionTypeKeys.HIGHLIGHT, new LuceneFieldHighlightProjection.Factory<>() );
			}

			if ( resolvedSortable ) {
				throw log.cannotUseAnalyzerOnSortableField( analyzerName, buildContext.getEventContext() );
			}

			if ( normalizer != null ) {
				throw log.cannotApplyAnalyzerAndNormalizer( analyzerName, normalizerName, buildContext.getEventContext() );
			}

			if ( indexNullAsValue != null ) {
				throw log.cannotUseIndexNullAsAndAnalyzer( analyzerName, indexNullAsValue, buildContext.getEventContext() );
			}

			if ( resolvedAggregable ) {
				throw log.cannotUseAnalyzerOnAggregableField( analyzerName, buildContext.getEventContext() );
			}
		}
		else {
			if ( normalizer != null ) {
				builder.normalizerName( normalizerName );
				builder.searchAnalyzerName( searchAnalyzerName );
				builder.indexingAnalyzerOrNormalizer( normalizer );
				builder.searchAnalyzerOrNormalizer( searchAnalyzer != null ? searchAnalyzer : normalizer );
			}
			else {
				builder.indexingAnalyzerOrNormalizer( AnalyzerConstants.KEYWORD_ANALYZER );
				builder.searchAnalyzerOrNormalizer( AnalyzerConstants.KEYWORD_ANALYZER );
			}

			if ( searchAnalyzer != null ) {
				throw log.searchAnalyzerWithoutAnalyzer( searchAnalyzerName, buildContext.getEventContext() );
			}
		}

		LuceneStringFieldCodec codec = new LuceneStringFieldCodec(
				getFieldType( resolvedProjectable, resolvedSearchable, analyzer != null, resolvedNorms, resolvedTermVector ),
				docValues,
				indexNullAsValue,
				builder.indexingAnalyzerOrNormalizer()
		);
		builder.codec( codec );

		if ( resolvedSearchable ) {
			builder.searchable( true );
			builder.queryElementFactory( PredicateTypeKeys.MATCH, new LuceneTextMatchPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.RANGE, new LuceneTextRangePredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.TERMS, new LuceneTextTermsPredicate.Factory<>( codec ) );
			builder.queryElementFactory( PredicateTypeKeys.EXISTS,
					resolvedNorms || DocValues.ENABLED.equals( docValues )
							? new LuceneExistsPredicate.DocValuesOrNormsBasedFactory<>()
							: new LuceneExistsPredicate.DefaultFactory<>() );
			builder.queryElementFactory( PredicateTypeKeys.PHRASE, new LuceneTextPhrasePredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.WILDCARD, new LuceneTextWildcardPredicate.Factory<>() );
			builder.queryElementFactory( PredicateTypeKeys.REGEXP, new LuceneTextRegexpPredicate.Factory<>() );
			builder.queryElementFactory( LucenePredicateTypeKeys.SIMPLE_QUERY_STRING,
					new LuceneCommonQueryStringPredicateBuilderFieldState.Factory() );
			builder.queryElementFactory( LucenePredicateTypeKeys.QUERY_STRING,
					new LuceneCommonQueryStringPredicateBuilderFieldState.Factory() );
		}

		if ( resolvedSortable ) {
			builder.sortable( true );
			builder.queryElementFactory( SortTypeKeys.FIELD, new LuceneStandardFieldSort.TextFieldFactory<>( codec ) );
		}

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new LuceneFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new LuceneTextTermsAggregation.Factory() );
		}

		return builder.build();
	}

	@Override
	protected LuceneStringIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

	private LuceneAnalysisDefinitionRegistry getAnalysisDefinitionRegistry() {
		return buildContext.getAnalysisDefinitionRegistry();
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
		TermVector localTermVector = termVector;
		if ( highlightable != null
				&& ( highlightable.contains( Highlightable.ANY )
						|| highlightable.contains( Highlightable.FAST_VECTOR ) ) ) {
			if ( TermVector.DEFAULT.equals( termVector ) ) {
				localTermVector = TermVector.WITH_POSITIONS_OFFSETS;
			}
			else if ( TermVector.WITH_POSITIONS_OFFSETS.equals( termVector )
					|| TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS.equals( termVector ) ) {
				localTermVector = termVector;
			}
			else {
				throw log.termVectorDontAllowFastVectorHighlighter( termVector );
			}
		}
		switch ( localTermVector ) {
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
				throw new AssertionFailure( "Unexpected value for TermVector: " + localTermVector );
		}
	}

	private static FieldType getFieldType(boolean projectable, boolean searchable, boolean analyzed, boolean norms,
			ResolvedTermVector termVector) {
		FieldType fieldType = new FieldType();

		if ( !searchable ) {
			if ( !projectable ) {
				return null;
			}
			fieldType.setIndexOptions( IndexOptions.NONE );
			fieldType.setStored( true );
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

		private void applyTo(FieldType fieldType) {
			fieldType.setStoreTermVectors( store );
			fieldType.setStoreTermVectorPositions( positions );
			fieldType.setStoreTermVectorOffsets( offsets );
			fieldType.setStoreTermVectorPayloads( payloads );
		}

		private boolean hasAnyEnabled() {
			return store || positions || offsets || payloads;
		}
	}

	private boolean resolveProjectable() {
		if ( highlightable != null && !highlightable.contains( Highlightable.NO ) ) {
			return true;
		}
		return resolveDefault( projectable );
	}

	private Set<SearchHighlighterType> resolveAllowedHighlighterTypes() {
		if ( highlightable == null ) {
			highlightable = EnumSet.of( Highlightable.DEFAULT );
		}
		if ( highlightable.isEmpty() ) {
			throw log.noHighlightableProvided();
		}
		if ( highlightable.contains( Highlightable.DEFAULT ) ) {
			// means we have the default case, so let's check if either plain or unified highlighters can be applied:
			if ( Projectable.YES.equals( projectable ) ) {
				if ( TermVector.WITH_POSITIONS_OFFSETS.equals( termVector )
						|| TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS.equals( termVector ) ) {
					highlightable = EnumSet.of( Highlightable.ANY );
				}
				else {
					highlightable = EnumSet.of( Highlightable.UNIFIED, Highlightable.PLAIN );
				}
			}
		}

		if ( highlightable.contains( Highlightable.NO ) ) {
			if ( highlightable.size() == 1 ) {
				return Collections.emptySet();
			}
			else {
				throw log.unsupportedMixOfHighlightableValues( highlightable );
			}
		}
		if ( highlightable.contains( Highlightable.ANY ) ) {
			return EnumSet.of( SearchHighlighterType.PLAIN, SearchHighlighterType.UNIFIED, SearchHighlighterType.FAST_VECTOR );
		}
		Set<SearchHighlighterType> highlighters = new HashSet<>();
		if ( highlightable.contains( Highlightable.PLAIN ) ) {
			highlighters.add( SearchHighlighterType.PLAIN );
		}
		if ( highlightable.contains( Highlightable.UNIFIED ) ) {
			highlighters.add( SearchHighlighterType.UNIFIED );
		}
		if ( highlightable.contains( Highlightable.FAST_VECTOR ) ) {
			highlighters.add( SearchHighlighterType.FAST_VECTOR );
		}

		return highlighters.isEmpty() ? Collections.emptySet() : EnumSet.copyOf( highlighters );
	}
}
