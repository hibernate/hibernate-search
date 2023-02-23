/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asImmutableList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsValuesDelegate;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterBuilder;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.LimitTokenOffsetFilter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.vectorhighlight.BoundaryScanner;
import org.apache.lucene.search.vectorhighlight.BreakIteratorBoundaryScanner;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.ScoreOrderFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleBoundaryScanner;
import org.apache.lucene.search.vectorhighlight.SimpleFieldFragList;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SingleFragListBuilder;

public class LuceneSearchHighlighter implements SearchHighlighter {

	public static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final Encoder DEFAULT_ENCODER = new DefaultEncoder();
	private static final Encoder HTML_ENCODER = new SimpleHTMLEncoder();
	private static final NullFragmenter NULL_FRAGMENTER = new NullFragmenter();
	public static final LuceneSearchHighlighter DEFAULTS_UNIFIED = defaultConfigurationSettings(
			SearchHighlighterType.UNIFIED,
			BoundaryScannerType.SENTENCE
	);

	private static final LuceneSearchHighlighter DEFAULTS_PLAIN = defaultConfigurationSettings(
			SearchHighlighterType.PLAIN,
			BoundaryScannerType.SENTENCE
	);

	private static final LuceneSearchHighlighter DEFAULTS_FV = defaultConfigurationSettings(
			SearchHighlighterType.FAST_VECTOR,
			BoundaryScannerType.CHARS
	);

	private static LuceneSearchHighlighter defaultConfigurationSettings(SearchHighlighterType highlighterType,
			BoundaryScannerType scannerType) {
		return new LuceneSearchHighlighter(
				Collections.emptySet(),
				highlighterType,
				Arrays.stream( SimpleBoundaryScanner.DEFAULT_BOUNDARY_CHARS ).map( String::valueOf )
						.collect( Collectors.joining() ),
				SimpleBoundaryScanner.DEFAULT_MAX_SCAN,
				100,
				0,
				5,
				false,
				null,
				Collections.singletonList( "<em>" ),
				Collections.singletonList( "</em>" ),
				scannerType,
				Locale.ROOT,
				HighlighterFragmenter.SPAN,
				256,
				DEFAULT_ENCODER
		);
	}

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
	private static final List<String> STYLED_SCHEMA_POST_TAGS = Collections.singletonList( "</em>" );

	private final Set<String> indexNames;
	private final SearchHighlighterType type;
	private final String boundaryChars;
	private final Integer boundaryMaxScan;
	private final Integer fragmentSize;
	private final Integer noMatchSize;
	private final Integer numberOfFragments;
	private final Boolean orderByScore;
	private final Integer maxAnalyzedOffset;
	private final List<String> preTags;
	private final List<String> postTags;
	private final BoundaryScannerType boundaryScannerType;
	private final Locale boundaryScannerLocale;
	private final HighlighterFragmenter fragmenterType;
	private final Integer phraseLimit;
	private final Encoder encoder;

	private LuceneSearchHighlighter(Builder builder) {
		this( builder.scope.hibernateSearchIndexNames(),
				SearchHighlighterType.DEFAULT.equals( builder.type() ) ? SearchHighlighterType.UNIFIED : builder.type(),
				builder.boundaryChars(), builder.boundaryMaxScan(),
				builder.fragmentSize(), builder.noMatchSize(), builder.numberOfFragments(), builder.orderByScore(),
				builder.maxAnalyzedOffset(),
				HighlighterTagSchema.STYLED.equals( builder.tagSchema() ) ? STYLED_SCHEMA_PRE_TAG : builder.preTags(),
				HighlighterTagSchema.STYLED.equals( builder.tagSchema() ) ? STYLED_SCHEMA_POST_TAGS :
						builder.postTags(),
				BoundaryScannerType.DEFAULT.equals( builder.boundaryScannerType() ) ? null : builder.boundaryScannerType(),
				builder.boundaryScannerLocale(), builder.fragmenterType(),
				builder.phraseLimit(),
				builder.encoder() != null ? (HighlighterEncoder.HTML.equals( builder.encoder() ) ? HTML_ENCODER : DEFAULT_ENCODER) : null
		);
	}

	private LuceneSearchHighlighter(Set<String> indexNames, SearchHighlighterType type, String boundaryChars,
			Integer boundaryMaxScan,
			Integer fragmentSize, Integer noMatchSize, Integer numberOfFragments, Boolean orderByScore,
			Integer maxAnalyzedOffset,
			List<String> preTags, List<String> postTags, BoundaryScannerType boundaryScannerType,
			Locale boundaryScannerLocale, HighlighterFragmenter fragmenterType,
			Integer phraseLimit,
			Encoder encoder
	) {
		this.indexNames = indexNames;
		this.type = type;
		this.boundaryChars = boundaryChars;
		this.boundaryMaxScan = boundaryMaxScan;
		this.fragmentSize = fragmentSize;
		this.noMatchSize = noMatchSize;
		this.numberOfFragments = numberOfFragments;
		this.orderByScore = orderByScore;
		this.maxAnalyzedOffset = maxAnalyzedOffset;
		this.preTags = preTags;
		this.postTags = postTags;
		this.boundaryScannerType = boundaryScannerType;
		this.boundaryScannerLocale = boundaryScannerLocale;
		this.fragmenterType = fragmenterType;
		this.phraseLimit = phraseLimit;
		this.encoder = encoder;
	}

	public static LuceneSearchHighlighter from(LuceneSearchIndexScope<?> scope, SearchHighlighter highlighter) {
		if ( !( highlighter instanceof LuceneSearchHighlighter ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherQueryHighlighters( highlighter );
		}
		LuceneSearchHighlighter casted = (LuceneSearchHighlighter) highlighter;
		if ( !scope.hibernateSearchIndexNames().equals( casted.indexNames() ) ) {
			throw log.queryHighlighterDefinedOnDifferentIndexes( highlighter, casted.indexNames(),
					scope.hibernateSearchIndexNames()
			);
		}
		return casted;
	}
	public LuceneSearchHighlighter withFallback(LuceneSearchHighlighter fallback) {
		Contracts.assertNotNull( fallback, "fallback highlighter" );

		if ( !this.type.equals( fallback.type ) ) {
			throw log.cannotMixDifferentHighlighterTypesAtOverrideLevel( this.type, fallback.type );
		}
		return new LuceneSearchHighlighter(
				indexNames,
				type != null ? type : fallback.type,
				boundaryChars != null ? boundaryChars : fallback.boundaryChars,
				boundaryMaxScan != null ? boundaryMaxScan : fallback.boundaryMaxScan,
				fragmentSize != null ? fragmentSize : fallback.fragmentSize,
				noMatchSize != null ? noMatchSize : fallback.noMatchSize,
				numberOfFragments != null ? numberOfFragments : fallback.numberOfFragments,
				orderByScore != null ? orderByScore : fallback.orderByScore,
				maxAnalyzedOffset != null ? maxAnalyzedOffset : fallback.maxAnalyzedOffset,
				preTags != null && !preTags.isEmpty() ? preTags : fallback.preTags,
				postTags != null && !postTags.isEmpty() ? postTags : fallback.postTags,
				boundaryScannerType != null ? boundaryScannerType : fallback.boundaryScannerType,
				boundaryScannerLocale != null ? boundaryScannerLocale : fallback.boundaryScannerLocale,
				fragmenterType != null ? fragmenterType : fallback.fragmenterType,
				phraseLimit != null ? phraseLimit : fallback.phraseLimit,
				encoder != null ? encoder : fallback.encoder
		);
	}

	public LuceneSearchHighlighter withFallbackDefaults() {
		switch ( this.type ) {
			case UNIFIED:
				return withFallback( DEFAULTS_UNIFIED );
			case PLAIN:
				return withFallback( DEFAULTS_PLAIN );
			case FAST_VECTOR:
				return withFallback( DEFAULTS_FV );
			default:
				throw new IllegalStateException( "Unknown highlighter type: " + this.type );
		}
	}

	public Set<String> indexNames() {
		return indexNames;
	}

	public Values<List<String>> createValues(String field, Analyzer analyzer, ProjectionExtractContext context) {
		switch ( this.type ) {
			case UNIFIED:
				return new UnifiedHighlighterValues( field, analyzer, context );
			case PLAIN:
				return new PlainHighlighterValues( field, analyzer, context );
			case FAST_VECTOR:
				return new FastVectorHighlighterValues( field, context );
			default:
				throw new IllegalStateException( "Unknown highlighter type: " + this.type );
		}
	}

	public static class Builder extends SearchHighlighterBuilder {

		private final LuceneSearchIndexScope<?> scope;

		public Builder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		public LuceneSearchHighlighter build() {
			return new LuceneSearchHighlighter( this );
		}
	}

	private final class PlainHighlighterValues implements Values<List<String>> {
		private final Highlighter highlighter;
		private final Analyzer analyzer;
		private final String field;
		private final StoredFieldsValuesDelegate storedFieldsValuesDelegate;

		PlainHighlighterValues(String field, Analyzer analyzer, ProjectionExtractContext context) {
			this.field = field;
			this.analyzer = LimitTokenOffsetAnalyzer.analyzer( analyzer, LuceneSearchHighlighter.this.maxAnalyzedOffset );
			this.storedFieldsValuesDelegate = context.collectorExecutionContext().storedFieldsValuesDelegate();

			QueryScorer queryScorer = new QueryScorer( context.collectorExecutionContext().executedQuery(), field );
			queryScorer.setExpandMultiTermQuery( true );

			Fragmenter fragmenter;

			if ( LuceneSearchHighlighter.this.numberOfFragments == 0 ) {
				fragmenter = NULL_FRAGMENTER;
			}
			else if ( HighlighterFragmenter.SPAN.equals( LuceneSearchHighlighter.this.fragmenterType ) ) {
				fragmenter = new SimpleSpanFragmenter( queryScorer, LuceneSearchHighlighter.this.fragmentSize );
			}
			else {
				fragmenter = new SimpleFragmenter( LuceneSearchHighlighter.this.fragmentSize );
			}

			this.highlighter = new Highlighter(
					new SimpleHTMLFormatter( LuceneSearchHighlighter.this.preTags.get( 0 ), LuceneSearchHighlighter.this.postTags.get( 0 ) ),
					LuceneSearchHighlighter.this.encoder,
					queryScorer
			);
			this.highlighter.setTextFragmenter( fragmenter );
			this.highlighter.setMaxDocCharsToAnalyze( Integer.MAX_VALUE );
		}

		@Override
		public void context(LeafReaderContext context) {
			// do nothing
		}

		@Override
		public List<String> get(int doc) throws IOException {
			try {
				// as fields can be multivalued we want to iterate over all the values
				IndexableField[] indexableFields = storedFieldsValuesDelegate.get( doc ).getFields( field );
				if ( indexableFields != null ) {
					// we build a single result based on all field entries:
					List<TextFragment> result = new ArrayList<>();
					String text = null;
					for ( IndexableField indexableField : indexableFields ) {
						text = indexableField.stringValue();
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
						if ( LuceneSearchHighlighter.this.orderByScore ) {
							result.sort( Comparator.comparingDouble( TextFragment::getScore ).reversed() );
						}
						List<String> converted = new ArrayList<>( result.size() );
						for ( TextFragment textFragment : result ) {
							converted.add( textFragment.toString() );
						}
						return converted;
					}
					else if ( LuceneSearchHighlighter.this.noMatchSize > 0 && text != null ) {
						return Collections.singletonList( text.substring( 0, Math.min( LuceneSearchHighlighter.this.noMatchSize, text.length() ) ) );
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

	private final class UnifiedHighlighterValues implements Values<List<String>> {

		private final String[] fieldsIn;
		private final int[] maxPassagesIn;
		private final Analyzer analyzer;
		private final Query query;
		private final PassageFormatterWithEncoder formatter;
		private LeafReaderContext leafReaderContext;

		UnifiedHighlighterValues(String field, Analyzer analyzer, ProjectionExtractContext context) {
			this.fieldsIn = new String[] { field };
			this.maxPassagesIn = new int[] { LuceneSearchHighlighter.this.numberOfFragments };
			this.analyzer = LimitTokenOffsetAnalyzer.analyzer( analyzer, LuceneSearchHighlighter.this.maxAnalyzedOffset );
			this.query = context.collectorExecutionContext().executedQuery();
			this.formatter = new PassageFormatterWithEncoder(
					LuceneSearchHighlighter.this.preTags.get( 0 ),
					LuceneSearchHighlighter.this.postTags.get( 0 ),
					LuceneSearchHighlighter.this.encoder
			);
		}

		private BreakIterator breakIterator() {
			// fragmentSize is ignored in both cases. In Elasticsearch this is done with  their custom `BoundedBreakIteratorScanner`
			if ( BoundaryScannerType.WORD.equals( LuceneSearchHighlighter.this.boundaryScannerType ) ) {
				return BreakIterator.getWordInstance( LuceneSearchHighlighter.this.boundaryScannerLocale );
			}
			else if ( BoundaryScannerType.SENTENCE.equals( LuceneSearchHighlighter.this.boundaryScannerType ) ) {
				return BreakIterator.getSentenceInstance( LuceneSearchHighlighter.this.boundaryScannerLocale );
			}

			throw log.unsupportedBoundaryScannerType( LuceneSearchHighlighter.this.type, LuceneSearchHighlighter.this.boundaryScannerType );
		}

		@Override
		public void context(LeafReaderContext context) {
			// store the leaf reader so that we can pass it to highlighter later.
			// using a global index searcher doesn't work as the docs passed to #get() do not match the TopDocs
			// accessible through the ProjectionExtractContext.
			this.leafReaderContext = context;
		}

		@Override
		public List<String> get(int doc) throws IOException {
			UnifiedHighlighter highlighter = new UnifiedHighlighter(
					new IndexSearcher( leafReaderContext.reader() ),
					this.analyzer
			);
			highlighter.setFormatter( formatter );
			highlighter.setBreakIterator( this::breakIterator );
			// to correctly support no match size we need to override UnifiedHighlighter#getFieldHighlighter() so that
			// we can return our own custom field highlighter that would cut the text where we need rather than returning
			// entire field value...
			highlighter.setMaxNoHighlightPassages( LuceneSearchHighlighter.this.noMatchSize > 0 ? 1 : 0 );

			// there is a variation of highlighting highlighter.highlightWithoutSearcher(  ) ...
			// wonder if we should use it and don't rely on IndexReader ?
			Map<String, String[]> highlights = highlighter.highlightFields(
					fieldsIn, query, new int[] { doc }, maxPassagesIn );

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
					false // doesn't really matter as we override append() to use encoder rather than rely on this property
			);
			this.encoder = encoder;
		}

		@Override
		protected void append(StringBuilder dest, String content, int start, int end) {
			dest.append( encoder.encodeText( content.substring( start, end ) ) );
		}
	}

	private static class LimitTokenOffsetAnalyzer extends AnalyzerWrapper {

		static Analyzer analyzer(Analyzer delegate, Integer maxOffset) {
			if ( maxOffset != null ) {
				return new LimitTokenOffsetAnalyzer( delegate, maxOffset );
			}
			return delegate;
		}

		private final Analyzer delegate;
		private final int maxOffset;

		public LimitTokenOffsetAnalyzer(Analyzer delegate, int maxOffset) {
			super( delegate.getReuseStrategy() );
			this.delegate = delegate;
			this.maxOffset = maxOffset;
		}

		@Override
		protected Analyzer getWrappedAnalyzer(String fieldName) {
			return delegate;
		}

		@Override
		protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
			return new TokenStreamComponents(
					components.getSource(),
					new LimitTokenOffsetFilter( components.getTokenStream(), maxOffset, false )
			);
		}
	}

	private final class FastVectorHighlighterValues implements Values<List<String>> {
		private final FastVectorHighlighter highlighter;
		private final String field;
		private final FieldQuery fieldQuery;
		private final FragListBuilder fragListBuilder;
		private final FragmentsBuilder fragmentsBuilder;
		private final String[] preTags;
		private final String[] postTags;
		private final Integer maxNumFragments;
		private LeafReaderContext leafReaderContext;

		FastVectorHighlighterValues(String field, ProjectionExtractContext context) {
			this.field = field;

			this.highlighter = new FastVectorHighlighter();
			this.highlighter.setPhraseLimit( LuceneSearchHighlighter.this.phraseLimit );
			this.fieldQuery = highlighter.getFieldQuery( context.collectorExecutionContext().executedQuery() );
			this.fragListBuilder = LuceneSearchHighlighter.this.numberOfFragments == 0 ? new SingleFragListBuilder() :
					new SimpleFragListBuilder();

			this.preTags = LuceneSearchHighlighter.this.preTags.toArray( new String[LuceneSearchHighlighter.this.preTags.size()] );
			this.postTags = LuceneSearchHighlighter.this.postTags.toArray( new String[LuceneSearchHighlighter.this.postTags.size()] );

			BoundaryScanner boundaryScanner = boundaryScanner();
			if ( Boolean.TRUE.equals( LuceneSearchHighlighter.this.orderByScore ) ) {
				ScoreOrderFragmentsBuilder builder = new ScoreOrderFragmentsBuilder( this.preTags, this.postTags, boundaryScanner );
				builder.setDiscreteMultiValueHighlighting( true );
				this.fragmentsBuilder = builder;
			}
			else {
				SimpleFragmentsBuilder builder = new SimpleFragmentsBuilder( this.preTags, this.postTags, boundaryScanner );
				builder.setDiscreteMultiValueHighlighting( true );
				this.fragmentsBuilder = builder;
			}
			this.maxNumFragments = numberOfFragments > 0 ? numberOfFragments : Integer.MAX_VALUE;
		}

		private BoundaryScanner boundaryScanner() {
			switch ( LuceneSearchHighlighter.this.boundaryScannerType ) {
				case CHARS:
					Set<Character> boundaryChars = new HashSet<>();
					for ( char ch : LuceneSearchHighlighter.this.boundaryChars.toCharArray() ) {
						boundaryChars.add( Character.valueOf( ch ) );
					}
					return new SimpleBoundaryScanner(
							LuceneSearchHighlighter.this.boundaryMaxScan,
							boundaryChars
					);
				case SENTENCE:
					return new BreakIteratorBoundaryScanner(
							BreakIterator.getSentenceInstance( LuceneSearchHighlighter.this.boundaryScannerLocale )
					);
				case WORD:
					return new BreakIteratorBoundaryScanner(
							BreakIterator.getWordInstance( LuceneSearchHighlighter.this.boundaryScannerLocale )
					);
				default:
					throw log.unsupportedBoundaryScannerType( LuceneSearchHighlighter.this.type, LuceneSearchHighlighter.this.boundaryScannerType );
			}
		}

		@Override
		public void context(LeafReaderContext context) {
			// do nothing.
			this.leafReaderContext = context;
		}

		@Override
		public List<String> get(int doc) throws IOException {
			String[] bestFragments = highlighter.getBestFragments(
					fieldQuery,
					leafReaderContext.reader(),
					doc,
					field,
					LuceneSearchHighlighter.this.fragmentSize,
					maxNumFragments,
					fragListBuilder,
					fragmentsBuilder,
					preTags,
					postTags,
					LuceneSearchHighlighter.this.encoder
			);
			if ( bestFragments != null && bestFragments.length > 0 ) {
				return Arrays.asList( bestFragments );
			}
			else if ( LuceneSearchHighlighter.this.noMatchSize > 0 ) {
				SimpleFieldFragList fieldFragList = new SimpleFieldFragList( -1 );
				fieldFragList.add( 0, LuceneSearchHighlighter.this.noMatchSize, Collections.emptyList() );

				String[] fragment = fragmentsBuilder.createFragments(
						leafReaderContext.reader(), doc, field, fieldFragList, 1, preTags, postTags,
						LuceneSearchHighlighter.this.encoder
				);
				return fragment == null || fragment.length == 0 ? Collections.emptyList() : Collections.singletonList( fragment[0] );
			}
			return Collections.emptyList();
		}
	}

}
