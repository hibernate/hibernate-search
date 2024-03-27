/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldHighlightProjection.HighlighterValues;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.uhighlight.Passage;
import org.apache.lucene.search.uhighlight.PassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;

class LuceneUnifiedSearchHighlighter extends LuceneAbstractSearchHighlighter {

	private static final Comparator<TextFragment> SCORE_COMPARATOR = Comparator.comparingDouble( TextFragment::score )
			.reversed();
	public static final LuceneUnifiedSearchHighlighter DEFAULTS = new LuceneUnifiedSearchHighlighter(
			BoundaryScannerType.SENTENCE
	);

	private LuceneUnifiedSearchHighlighter(BoundaryScannerType scannerType) {
		super( scannerType );
	}

	protected LuceneUnifiedSearchHighlighter(Builder builder) {
		super( builder );
	}

	private LuceneUnifiedSearchHighlighter(Set<String> indexNames,
			Character[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType,
			Integer phraseLimit, Encoder encoder) {
		super( indexNames, boundaryChars, boundaryMaxScan, fragmentSize, noMatchSize, numberOfFragments,
				orderByScore,
				preTags, postTags, boundaryScannerType, boundaryScannerLocale, fragmenterType,
				phraseLimit, encoder
		);

		// to correctly support no match size we need to override UnifiedHighlighter#getFieldHighlighter() so that
		// we can return our own custom field highlighter that would cut the text where we need rather than returning
		// entire field value...
		if ( this.noMatchSize > 0 && this.noMatchSize != Integer.MAX_VALUE ) {
			// we don't want to throw an exception here as users might still want to get the no-match fragment even if
			// they cannot limit the length of it...
			log.unifiedHighlighterNoMatchSizeWarning( this.noMatchSize );
		}
	}

	@Override
	protected LuceneAbstractSearchHighlighter createHighlighterSameType(Set<String> indexNames,
			Character[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType, Integer phraseLimit, Encoder encoder) {
		return new LuceneUnifiedSearchHighlighter(
				indexNames, boundaryChars, boundaryMaxScan, fragmentSize, noMatchSize, numberOfFragments,
				orderByScore, preTags, postTags, boundaryScannerType, boundaryScannerLocale,
				fragmenterType, phraseLimit, encoder
		);
	}

	@Override
	public LuceneAbstractSearchHighlighter withFallbackDefaults() {
		return withFallback( DEFAULTS );
	}

	@Override
	public <A, T> Values<A> createValues(String parentDocumentPath, String nestedDocumentPath,
			String absoluteFieldPath, Analyzer analyzer, ProjectionExtractContext context,
			ProjectionAccumulator<String, ?, A, T> accumulator) {
		return new UnifiedHighlighterValues<>(
				parentDocumentPath, nestedDocumentPath, absoluteFieldPath, analyzer, context, accumulator );
	}

	@Override
	public SearchHighlighterType type() {
		return SearchHighlighterType.UNIFIED;
	}


	private final class UnifiedHighlighterValues<A, T> extends HighlighterValues<A, T> {

		private final String[] fieldsIn;
		private final int[] maxPassagesIn;
		private final Query query;
		private final MultiValueUnifiedHighlighter highlighter;

		UnifiedHighlighterValues(String parentDocumentPath, String nestedDocumentPath, String field, Analyzer analyzer,
				ProjectionExtractContext context, ProjectionAccumulator<String, ?, A, T> accumulator) {
			super( parentDocumentPath, nestedDocumentPath, context.collectorExecutionContext(), accumulator );
			this.fieldsIn = new String[] { field };
			this.maxPassagesIn = new int[] { LuceneUnifiedSearchHighlighter.this.numberOfFragments };
			this.query = context.collectorExecutionContext().originalQuery();
			PassageFormatterWithEncoder formatter = new PassageFormatterWithEncoder(
					LuceneUnifiedSearchHighlighter.this.preTags.get( 0 ),
					LuceneUnifiedSearchHighlighter.this.postTags.get( 0 ),
					LuceneUnifiedSearchHighlighter.this.encoder
			);

			this.highlighter =
					MultiValueUnifiedHighlighter.builder( context.collectorExecutionContext().getIndexSearcher(), analyzer )
							.withFormatter( formatter )
							.withBreakIterator( this::breakIterator )
							.withMaxNoHighlightPassages( LuceneUnifiedSearchHighlighter.this.noMatchSize > 0 ? 1 : 0 )
							.build();
		}

		private BreakIterator breakIterator() {
			// fragmentSize is ignored in both cases. In Elasticsearch this is done with  their custom `BoundedBreakIteratorScanner`
			if ( BoundaryScannerType.WORD.equals( LuceneUnifiedSearchHighlighter.this.boundaryScannerType ) ) {
				return BreakIterator.getWordInstance( LuceneUnifiedSearchHighlighter.this.boundaryScannerLocale );
			}
			else if ( BoundaryScannerType.SENTENCE.equals( LuceneUnifiedSearchHighlighter.this.boundaryScannerType ) ) {
				return BreakIterator.getSentenceInstance( LuceneUnifiedSearchHighlighter.this.boundaryScannerLocale );
			}

			throw log.unsupportedBoundaryScannerType(
					LuceneUnifiedSearchHighlighter.this.getClass().getSimpleName(),
					LuceneUnifiedSearchHighlighter.this.boundaryScannerType
			);
		}

		@Override
		public List<String> highlight(int doc) throws IOException {
			List<TextFragment> highlights =
					highlighter.highlightField( fieldsIn, query, leafReaderContext.docBase + doc, maxPassagesIn );
			if ( highlights == null ) {
				return Collections.emptyList();
			}
			else {
				if ( Boolean.TRUE.equals( LuceneUnifiedSearchHighlighter.this.orderByScore ) ) {
					highlights.sort( SCORE_COMPARATOR );
				}
				List<String> result = new ArrayList<>( highlights.size() );
				for ( TextFragment highlight : highlights ) {
					result.add( highlight.highlightedText() );
				}
				return result;
			}
		}
	}

	private static class MultiValueUnifiedHighlighter extends UnifiedHighlighter {

		private MultiValueUnifiedHighlighter(MultiValueUnifiedHighlighter.Builder builder) {
			super( builder );
		}


		@SuppressWarnings("unchecked")
		public List<TextFragment> highlightField(String[] fieldIn, Query query, int doc, int[] maxPassagesIn)
				throws IOException {
			assert fieldIn.length == 1;
			return (List<TextFragment>) highlightFieldsAsObjects( fieldIn, query, new int[] { doc }, maxPassagesIn )
					.get( fieldIn[0] )[0];
		}

		public static class Builder extends UnifiedHighlighter.Builder {

			/**
			 * Constructor for UH builder which accepts {@link IndexSearcher} and {@link Analyzer} objects.
			 * {@link IndexSearcher} object can only be null when {@link #highlightWithoutSearcher(String,
			 * Query, String, int)} is used.
			 *
			 * @param searcher - {@link IndexSearcher}
			 * @param indexAnalyzer - {@link Analyzer}
			 */
			public Builder(IndexSearcher searcher, Analyzer indexAnalyzer) {
				super( searcher, indexAnalyzer );
			}

			// with* methods overridden only to return the builder type we need.
			@Override
			public Builder withBreakIterator(Supplier<BreakIterator> value) {
				super.withBreakIterator( value );
				return this;
			}

			@Override
			public Builder withMaxNoHighlightPassages(int value) {
				super.withMaxNoHighlightPassages( value );
				return this;
			}

			@Override
			public Builder withFormatter(PassageFormatter value) {
				super.withFormatter( value );
				return this;
			}

			@Override
			public MultiValueUnifiedHighlighter build() {
				return new MultiValueUnifiedHighlighter( this );
			}
		}

		public static Builder builder(IndexSearcher searcher, Analyzer indexAnalyzer) {
			return new Builder( searcher, indexAnalyzer );
		}
	}

	/**
	 * `DefaultPassageFormatter` uses a string builder to create a resulting highlighted string.
	 * We'll keep passages separate, while keeping the remaining logic unchanged, to provide multiple fragments as a result of highlighting.
	 * <p>
	 * Some of this code was copied and adapted from
	 * {@code org.apache.lucene.search.uhighlight.DefaultPassageFormatter}
	 * of <a href="https://lucene.apache.org/">Apache Lucene project</a>.
	 */
	static class PassageFormatterWithEncoder extends PassageFormatter {
		private final String preTag;
		private final String postTag;
		private final Encoder encoder;

		public PassageFormatterWithEncoder(String preTag, String postTag, Encoder encoder) {
			this.preTag = preTag;
			this.postTag = postTag;
			this.encoder = encoder;
		}

		public List<TextFragment> format(Passage[] passages, String content) {
			List<TextFragment> result = new ArrayList<>( passages.length );

			int pos = 0;
			for ( Passage passage : passages ) {
				StringBuilder sb = new StringBuilder();
				pos = passage.getStartOffset();
				int start = 0;
				for ( int i = 0; i < passage.getNumMatches(); i++ ) {
					start = passage.getMatchStarts()[i];
					assert start >= pos && start < passage.getEndOffset();
					//append content before this start
					append( sb, content, pos, start );

					int end = passage.getMatchEnds()[i];
					assert end > start;
					// it's possible to have overlapping terms.
					//   Look ahead to expand 'end' past all overlapping:
					while ( i + 1 < passage.getNumMatches() && passage.getMatchStarts()[i + 1] < end ) {
						//CHECKSTYLE:OFF: ModifiedControlVariable - this comes from the Lucene lib and we'd better keep
						// changes to a minimum
						end = passage.getMatchEnds()[++i];
						//CHECKSTYLE:ON
					}
					end = Math.min( end, passage.getEndOffset() ); // in case match straddles past passage

					sb.append( preTag );
					append( sb, content, start, end );
					sb.append( postTag );

					pos = end;
				}
				// its possible a "term" from the analyzer could span a sentence boundary.
				append( sb, content, pos, Math.max( pos, passage.getEndOffset() ) );

				result.add( new TextFragment( sb.toString().trim(), passage.getScore() ) );
			}
			return result;
		}

		protected void append(StringBuilder dest, String content, int start, int end) {
			dest.append( encoder.encodeText( content.substring( start, end ) ) );
		}
	}

	static class TextFragment {

		private final String highlightedText;
		private final float score;

		public TextFragment(String highlightedText, float score) {
			this.highlightedText = highlightedText;
			this.score = score;
		}

		public String highlightedText() {
			return highlightedText;
		}

		public float score() {
			return score;
		}
	}
}
