/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asImmutableList;
import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.vectorhighlight.SimpleBoundaryScanner;

public abstract class LuceneAbstractSearchHighlighter implements SearchHighlighter {

	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final Encoder DEFAULT_ENCODER = new DefaultEncoder();
	private static final Encoder HTML_ENCODER = new SimpleHTMLEncoder();

	private static final List<String> DEFAULT_PRE_TAGS = Collections.singletonList( "<em>" );
	private static final List<String> DEFAULT_POST_TAGS = Collections.singletonList( "</em>" );
	private static final List<String> STYLED_SCHEMA_PRE_TAG = asImmutableList(
			"<em class=\"hlt1\">",
			"<em class=\"hlt2\">",
			"<em class=\"hlt3\">",
			"<em class=\"hlt4\">",
			"<em class=\"hlt5\">",
			"<em class=\"hlt6\">",
			"<em class=\"hlt7\">",
			"<em class=\"hlt8\">",
			"<em class=\"hlt9\">",
			"<em class=\"hlt10\">"
	);
	private static final List<String> STYLED_SCHEMA_POST_TAGS = DEFAULT_POST_TAGS;

	protected final Set<String> indexNames;
	protected final Character[] boundaryChars;
	protected final Integer boundaryMaxScan;
	protected final Integer fragmentSize;
	protected final Integer noMatchSize;
	protected final Integer numberOfFragments;
	protected final Boolean orderByScore;
	protected final List<String> preTags;
	protected final List<String> postTags;
	protected final BoundaryScannerType boundaryScannerType;
	protected final Locale boundaryScannerLocale;
	protected final HighlighterFragmenter fragmenterType;
	protected final Integer phraseLimit;
	protected final Encoder encoder;

	protected LuceneAbstractSearchHighlighter(Builder builder) {
		this( builder.scope.hibernateSearchIndexNames(),
				builder.boundaryChars(), builder.boundaryMaxScan(),
				builder.fragmentSize(), builder.noMatchSize(), builder.numberOfFragments(), builder.orderByScore(),
				HighlighterTagSchema.STYLED.equals( builder.tagSchema() ) ? STYLED_SCHEMA_PRE_TAG : builder.preTags(),
				HighlighterTagSchema.STYLED.equals( builder.tagSchema() ) ? STYLED_SCHEMA_POST_TAGS : builder.postTags(),
				BoundaryScannerType.DEFAULT.equals( builder.boundaryScannerType() ) ? null : builder.boundaryScannerType(),
				builder.boundaryScannerLocale(), builder.fragmenterType(),
				builder.phraseLimit(),
				builder.encoder() != null
						? ( HighlighterEncoder.HTML.equals( builder.encoder() ) ? HTML_ENCODER : DEFAULT_ENCODER )
						: null
		);
	}

	protected LuceneAbstractSearchHighlighter(BoundaryScannerType scannerType) {
		this(
				Collections.emptySet(),
				SimpleBoundaryScanner.DEFAULT_BOUNDARY_CHARS,
				SimpleBoundaryScanner.DEFAULT_MAX_SCAN,
				100,
				0,
				5,
				false,
				DEFAULT_PRE_TAGS,
				DEFAULT_POST_TAGS,
				scannerType,
				Locale.ROOT,
				HighlighterFragmenter.SPAN,
				256,
				DEFAULT_ENCODER
		);
	}

	protected LuceneAbstractSearchHighlighter(Set<String> indexNames,
			Character[] boundaryChars,
			Integer boundaryMaxScan,
			Integer fragmentSize, Integer noMatchSize, Integer numberOfFragments, Boolean orderByScore,
			List<String> preTags, List<String> postTags, BoundaryScannerType boundaryScannerType,
			Locale boundaryScannerLocale, HighlighterFragmenter fragmenterType,
			Integer phraseLimit,
			Encoder encoder
	) {
		this.indexNames = indexNames;
		this.boundaryChars = boundaryChars;
		this.boundaryMaxScan = boundaryMaxScan;
		this.fragmentSize = fragmentSize;
		this.noMatchSize = noMatchSize;
		this.numberOfFragments = numberOfFragments;
		this.orderByScore = orderByScore;
		this.preTags = preTags;
		this.postTags = postTags;
		this.boundaryScannerType = boundaryScannerType;
		this.boundaryScannerLocale = boundaryScannerLocale;
		this.fragmenterType = fragmenterType;
		this.phraseLimit = phraseLimit;
		this.encoder = encoder;
	}

	public static LuceneAbstractSearchHighlighter from(LuceneSearchIndexScope<?> scope, SearchHighlighter highlighter) {
		if ( !( highlighter instanceof LuceneAbstractSearchHighlighter ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherQueryHighlighters( highlighter );
		}
		LuceneAbstractSearchHighlighter casted = (LuceneAbstractSearchHighlighter) highlighter;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw log.queryHighlighterDefinedOnDifferentIndexes( highlighter, casted.indexNames(),
					scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() ) );
		}
		return casted;
	}

	public static LuceneAbstractSearchHighlighter defaultHighlighter() {
		return LuceneUnifiedSearchHighlighter.DEFAULTS;
	}

	public LuceneAbstractSearchHighlighter withFallback(LuceneAbstractSearchHighlighter fallback) {
		Contracts.assertNotNull( fallback, "fallback highlighter" );

		if ( !this.type().equals( fallback.type() ) ) {
			throw log.cannotMixDifferentHighlighterTypesAtOverrideLevel( this.type(), fallback.type() );
		}
		return createHighlighterSameType(
				indexNames,
				boundaryChars != null && boundaryChars.length != 0 ? boundaryChars : fallback.boundaryChars,
				boundaryMaxScan != null ? boundaryMaxScan : fallback.boundaryMaxScan,
				fragmentSize != null ? fragmentSize : fallback.fragmentSize,
				noMatchSize != null ? noMatchSize : fallback.noMatchSize,
				numberOfFragments != null ? numberOfFragments : fallback.numberOfFragments,
				orderByScore != null ? orderByScore : fallback.orderByScore,
				preTags != null && !preTags.isEmpty() ? preTags : fallback.preTags,
				postTags != null && !postTags.isEmpty() ? postTags : fallback.postTags,
				boundaryScannerType != null ? boundaryScannerType : fallback.boundaryScannerType,
				boundaryScannerLocale != null ? boundaryScannerLocale : fallback.boundaryScannerLocale,
				fragmenterType != null ? fragmenterType : fallback.fragmenterType,
				phraseLimit != null ? phraseLimit : fallback.phraseLimit,
				encoder != null ? encoder : fallback.encoder
		);
	}

	protected abstract LuceneAbstractSearchHighlighter createHighlighterSameType(Set<String> indexNames,
			Character[] boundaryChars,
			Integer boundaryMaxScan,
			Integer fragmentSize, Integer noMatchSize, Integer numberOfFragments, Boolean orderByScore,
			List<String> preTags, List<String> postTags, BoundaryScannerType boundaryScannerType,
			Locale boundaryScannerLocale, HighlighterFragmenter fragmenterType,
			Integer phraseLimit,
			Encoder encoder);


	public abstract LuceneAbstractSearchHighlighter withFallbackDefaults();

	public Set<String> indexNames() {
		return indexNames;
	}

	public abstract <A, T> Values<A> createValues(String parentDocumentPath, String nestedDocumentPath,
			String absoluteFieldPath, Analyzer analyzer, ProjectionExtractContext context,
			ProjectionAccumulator<String, ?, A, T> accumulator);

	public boolean isCompatible(ProjectionAccumulator.Provider<?, ?> provider) {
		return !provider.isSingleValued()
				|| ( provider.isSingleValued() && ( numberOfFragments != null && numberOfFragments.equals( 1 ) ) );
	}

	public abstract SearchHighlighterType type();

	public static class Builder extends SearchHighlighterBuilder {

		private final LuceneSearchIndexScope<?> scope;

		public Builder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		@Override
		public SearchHighlighterBuilder fragmentSize(Integer fragmentSize) {
			if ( SearchHighlighterType.UNIFIED.equals( type() ) ) {
				throw log.unifiedHighlighterFragmentSizeNotSupported();
			}
			return super.fragmentSize( fragmentSize );
		}

		public LuceneAbstractSearchHighlighter build() {
			switch ( this.type ) {
				case UNIFIED:
					return new LuceneUnifiedSearchHighlighter( this );
				case PLAIN:
					return new LucenePlainSearchHighlighter( this );
				case FAST_VECTOR:
					return new LuceneFastVectorSearchHighlighter( this );
				default:
					throw new IllegalStateException( "Unknown highlighter type." );
			}
		}
	}

	public void request(ProjectionRequestContext context, String absoluteFieldPath) {
		// do nothing
	}

}
