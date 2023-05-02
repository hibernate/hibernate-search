/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldHighlightProjection.HighlighterValues;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;

class LuceneUnifiedSearchHighlighter extends LuceneAbstractSearchHighlighter {

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
	public <A> Values<A> createValues(String parentDocumentPath, String nestedDocumentPath,
			String absoluteFieldPath, Analyzer analyzer, ProjectionExtractContext context,
			ProjectionAccumulator<String, ?, A, List<String>> accumulator) {
		return new UnifiedHighlighterValues<>(
				parentDocumentPath, nestedDocumentPath, absoluteFieldPath, analyzer, context, accumulator );
	}

	@Override
	public SearchHighlighterType type() {
		return SearchHighlighterType.UNIFIED;
	}


	private final class UnifiedHighlighterValues<A> extends HighlighterValues<A> {

		private final String[] fieldsIn;
		private final int[] maxPassagesIn;
		private final Query query;
		private final UnifiedHighlighter highlighter;

		UnifiedHighlighterValues(String parentDocumentPath, String nestedDocumentPath, String field, Analyzer analyzer,
				ProjectionExtractContext context, ProjectionAccumulator<String, ?, A, List<String>> accumulator) {
			super( parentDocumentPath, nestedDocumentPath, context.collectorExecutionContext(), accumulator );
			this.fieldsIn = new String[] { field };
			this.maxPassagesIn = new int[] { LuceneUnifiedSearchHighlighter.this.numberOfFragments };
			this.query = context.collectorExecutionContext().originalQuery();
			PassageFormatterWithEncoder formatter = new PassageFormatterWithEncoder(
					LuceneUnifiedSearchHighlighter.this.preTags.get( 0 ),
					LuceneUnifiedSearchHighlighter.this.postTags.get( 0 ),
					LuceneUnifiedSearchHighlighter.this.encoder
			);

			this.highlighter = new UnifiedHighlighter( context.collectorExecutionContext().getIndexSearcher(), analyzer );
			highlighter.setFormatter( formatter );
			highlighter.setBreakIterator( this::breakIterator );
			highlighter.setMaxNoHighlightPassages( LuceneUnifiedSearchHighlighter.this.noMatchSize > 0 ? 1 : 0 );
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
			Map<String, String[]> highlights = highlighter.highlightFields(
					fieldsIn, query, new int[] { leafReaderContext.docBase + doc }, maxPassagesIn );

			List<String> result = new ArrayList<>();
			for ( String highlight : highlights.get( fieldsIn[0] ) ) {
				if ( highlight != null ) {
					result.add( highlight );
				}
			}
			return result;
		}
	}

	// `DefaultPassageFormatter` uses a string builder to create a resulting highlighted string.
	// Elasticsearch uses their own formatter that creates an array of snippets instead.
	// Hence, the results won't match if more than one snipped is needed to provide the results.
	private static class PassageFormatterWithEncoder extends DefaultPassageFormatter {
		private final Encoder encoder;

		public PassageFormatterWithEncoder(String preTag, String postTag, Encoder encoder) {
			super( preTag, postTag,
					"", // don't do any ellipsis to mimic the Elasticsearch behavior
					false
					// doesn't really matter as we override append() to use encoder rather than rely on this property
			);
			this.encoder = encoder;
		}

		@Override
		protected void append(StringBuilder dest, String content, int start, int end) {
			dest.append( encoder.encodeText( content.substring( start, end ) ) );
		}
	}
}
