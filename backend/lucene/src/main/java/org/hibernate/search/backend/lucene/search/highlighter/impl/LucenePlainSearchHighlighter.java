/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldHighlightProjection.HighlighterValues;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionRequestContext;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;

class LucenePlainSearchHighlighter extends LuceneAbstractSearchHighlighter {

	private static final NullFragmenter NULL_FRAGMENTER = new NullFragmenter();
	private static final LucenePlainSearchHighlighter DEFAULTS = new LucenePlainSearchHighlighter(
			BoundaryScannerType.SENTENCE
	);

	private LucenePlainSearchHighlighter(BoundaryScannerType scannerType) {
		super( scannerType );
	}

	protected LucenePlainSearchHighlighter(Builder builder) {
		super( builder );
	}

	private LucenePlainSearchHighlighter(Set<String> indexNames,
			Character[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType, Integer phraseLimit,
			Encoder encoder) {
		super( indexNames, boundaryChars, boundaryMaxScan, fragmentSize, noMatchSize, numberOfFragments,
				orderByScore,
				preTags, postTags, boundaryScannerType, boundaryScannerLocale, fragmenterType,
				phraseLimit, encoder
		);
	}

	@Override
	protected LuceneAbstractSearchHighlighter createHighlighterSameType(Set<String> indexNames,
			Character[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType, Integer phraseLimit, Encoder encoder) {
		return new LucenePlainSearchHighlighter(
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
		return new PlainHighlighterValues<>(
				parentDocumentPath, nestedDocumentPath, absoluteFieldPath, analyzer, context, accumulator );
	}

	@Override
	public SearchHighlighterType type() {
		return SearchHighlighterType.PLAIN;
	}

	@Override
	public void request(ProjectionRequestContext context, String absoluteFieldPath) {
		context.requireStoredField( absoluteFieldPath, context.absoluteCurrentNestedFieldPath() );
	}

	private final class PlainHighlighterValues<A> extends HighlighterValues<A> {
		private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;
		private final Highlighter highlighter;
		private final Analyzer analyzer;
		private final String field;

		PlainHighlighterValues(String parentDocumentPath, String nestedDocumentPath, String field, Analyzer analyzer,
				ProjectionExtractContext context, ProjectionAccumulator<String, ?, A, List<String>> accumulator) {
			super( parentDocumentPath, nestedDocumentPath, context.collectorExecutionContext(), accumulator );
			this.storedFieldsValuesDelegate = context.collectorExecutionContext().storedFieldsValuesDelegate();
			this.field = field;
			this.analyzer = analyzer;

			QueryScorer queryScorer = new QueryScorer( context.collectorExecutionContext().originalQuery(), field );
			queryScorer.setExpandMultiTermQuery( true );

			Fragmenter fragmenter;

			if ( LucenePlainSearchHighlighter.this.numberOfFragments == 0 ) {
				fragmenter = NULL_FRAGMENTER;
			}
			else if ( HighlighterFragmenter.SPAN.equals( LucenePlainSearchHighlighter.this.fragmenterType ) ) {
				fragmenter = new SimpleSpanFragmenter( queryScorer, LucenePlainSearchHighlighter.this.fragmentSize );
			}
			else {
				fragmenter = new SimpleFragmenter( LucenePlainSearchHighlighter.this.fragmentSize );
			}

			this.highlighter = new Highlighter(
					new SimpleHTMLFormatter(
							LucenePlainSearchHighlighter.this.preTags.get( 0 ),
							LucenePlainSearchHighlighter.this.postTags.get( 0 )
					),
					LucenePlainSearchHighlighter.this.encoder,
					queryScorer
			);
			this.highlighter.setTextFragmenter( fragmenter );
			this.highlighter.setMaxDocCharsToAnalyze( Integer.MAX_VALUE );
		}

		@Override
		public List<String> highlight(int doc) throws IOException {
			try {
				// we build a single result based on all field entries:
				List<TextFragment> result = new ArrayList<>();
				for ( IndexableField indexableField : storedFieldsValuesDelegate.get( doc ).getFields() ) {
					if ( !indexableField.name().equals( field ) ) {
						continue;
					}
					String text = indexableField.stringValue();
					// we cannot use other highlight methods as we need to not merge the fragments and that's
					// the only method that would allow us to do so:
					TextFragment[] bestFragments = highlighter.getBestTextFragments(
							analyzer.tokenStream( field, text ),
							text,
							false,
							numberOfFragments
					);
					for ( int i = 0; i < bestFragments.length; i++ ) {
						if ( ( bestFragments[i] != null ) && ( bestFragments[i].getScore() > 0 ) ) {
							result.add( bestFragments[i] );
						}
					}
				}
				if ( !result.isEmpty() ) {
					if ( Boolean.TRUE.equals( LucenePlainSearchHighlighter.this.orderByScore ) ) {
						result.sort( Comparator.comparingDouble( TextFragment::getScore ).reversed() );
					}
					List<String> converted = new ArrayList<>( result.size() );
					for ( TextFragment textFragment : result ) {
						converted.add( textFragment.toString() );
					}
					return converted;
				}
				else if ( LucenePlainSearchHighlighter.this.noMatchSize > 0 ) {
					for ( IndexableField indexableField : storedFieldsValuesDelegate.get( doc ).getFields() ) {
						if ( !indexableField.name().equals( field ) ) {
							continue;
						}
						String text = indexableField.stringValue();
						if ( text.length() > 0 ) {
							return Collections.singletonList( text.substring(
									0,
									Math.min( LucenePlainSearchHighlighter.this.noMatchSize, text.length() )
							) );
						}
					}
				}
				return Collections.emptyList();
			}
			catch (InvalidTokenOffsetsException e) {
				// let's just propagate the exception to be handled as any other related to reading documents
				throw new IOException( e );
			}
		}
	}
}
