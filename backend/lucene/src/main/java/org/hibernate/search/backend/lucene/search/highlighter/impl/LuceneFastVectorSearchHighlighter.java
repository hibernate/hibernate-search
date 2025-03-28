/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.highlighter.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldHighlightProjection.HighlighterValues;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.spi.BoundaryScannerType;
import org.hibernate.search.engine.search.highlighter.spi.SearchHighlighterType;
import org.hibernate.search.engine.search.projection.ProjectionCollector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.BoundaryScanner;
import org.apache.lucene.search.vectorhighlight.BreakIteratorBoundaryScanner;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.ScoreOrderFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleBoundaryScanner;
import org.apache.lucene.search.vectorhighlight.SimpleFieldFragList;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SingleFragListBuilder;

class LuceneFastVectorSearchHighlighter extends LuceneAbstractSearchHighlighter {
	private static final LuceneFastVectorSearchHighlighter DEFAULTS = new LuceneFastVectorSearchHighlighter(
			BoundaryScannerType.CHARS
	);

	private LuceneFastVectorSearchHighlighter(BoundaryScannerType scannerType) {
		super( scannerType );
	}

	protected LuceneFastVectorSearchHighlighter(Builder builder) {
		super( builder );
	}

	private LuceneFastVectorSearchHighlighter(Set<String> indexNames,
			char[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType,
			Integer phraseLimit, Encoder encoder) {
		super( indexNames, boundaryChars, boundaryMaxScan, fragmentSize, noMatchSize, numberOfFragments,
				orderByScore,
				preTags, postTags, boundaryScannerType, boundaryScannerLocale, fragmenterType,
				phraseLimit, encoder
		);
	}

	@Override
	protected LuceneAbstractSearchHighlighter createHighlighterSameType(Set<String> indexNames,
			char[] boundaryChars, Integer boundaryMaxScan, Integer fragmentSize, Integer noMatchSize,
			Integer numberOfFragments, Boolean orderByScore, List<String> preTags,
			List<String> postTags, BoundaryScannerType boundaryScannerType, Locale boundaryScannerLocale,
			HighlighterFragmenter fragmenterType, Integer phraseLimit, Encoder encoder) {
		return new LuceneFastVectorSearchHighlighter(
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
			ProjectionCollector<String, ?, A, T> collector) {
		return new FastVectorHighlighterValues<>( parentDocumentPath, nestedDocumentPath, absoluteFieldPath, context,
				collector
		);
	}

	@Override
	public SearchHighlighterType type() {
		return SearchHighlighterType.FAST_VECTOR;
	}

	private final class FastVectorHighlighterValues<A, T> extends HighlighterValues<A, T> {
		private final FastVectorHighlighter highlighter;
		private final String field;
		private final Query query;
		private final FragListBuilder fragListBuilder;
		private final FragmentsBuilder fragmentsBuilder;
		private final FragmentsBuilder noMatchFragments;
		private final String[] preTags;
		private final String[] postTags;
		private final Integer maxNumFragments;

		FastVectorHighlighterValues(String parentDocumentPath, String nestedDocumentPath, String field,
				ProjectionExtractContext context, ProjectionCollector<String, ?, A, T> collector) {
			super( parentDocumentPath, nestedDocumentPath, context.collectorExecutionContext(), collector );
			this.field = field;

			this.highlighter = new FastVectorHighlighter();
			this.highlighter.setPhraseLimit( LuceneFastVectorSearchHighlighter.this.phraseLimit );
			this.query = context.collectorExecutionContext().originalQuery();
			this.fragListBuilder =
					LuceneFastVectorSearchHighlighter.this.numberOfFragments == 0
							? new SingleFragListBuilder()
							: new SimpleFragListBuilder();

			this.preTags = LuceneFastVectorSearchHighlighter.this.preTags.toArray(
					new String[LuceneFastVectorSearchHighlighter.this.preTags.size()] );
			this.postTags = LuceneFastVectorSearchHighlighter.this.postTags.toArray(
					new String[LuceneFastVectorSearchHighlighter.this.postTags.size()] );

			BoundaryScanner boundaryScanner = boundaryScanner();
			if ( Boolean.TRUE.equals( LuceneFastVectorSearchHighlighter.this.orderByScore ) ) {
				ScoreOrderFragmentsBuilder builder = new CustomScoreOrderFragmentsBuilder(
						this.preTags, this.postTags, boundaryScanner );
				builder.setDiscreteMultiValueHighlighting( true );
				this.fragmentsBuilder = builder;
			}
			else {
				SimpleFragmentsBuilder builder = new SimpleFragmentsBuilder( this.preTags, this.postTags, boundaryScanner );
				builder.setDiscreteMultiValueHighlighting( true );
				this.fragmentsBuilder = builder;
			}
			this.noMatchFragments = new NoMatchFragmentsBuilder( this.preTags, this.postTags, boundaryScanner );
			this.maxNumFragments = numberOfFragments > 0 ? numberOfFragments : Integer.MAX_VALUE;
		}

		private BoundaryScanner boundaryScanner() {
			switch ( LuceneFastVectorSearchHighlighter.this.boundaryScannerType ) {
				case CHARS:
					return new HibernateSearchSimpleBoundaryScanner(
							LuceneFastVectorSearchHighlighter.this.boundaryMaxScan,
							LuceneFastVectorSearchHighlighter.this.boundaryChars
					);
				case SENTENCE:
					return new BreakIteratorBoundaryScanner(
							BreakIterator.getSentenceInstance(
									LuceneFastVectorSearchHighlighter.this.boundaryScannerLocale )
					);
				case WORD:
					return new BreakIteratorBoundaryScanner(
							BreakIterator.getWordInstance(
									LuceneFastVectorSearchHighlighter.this.boundaryScannerLocale )
					);
				default:
					throw QueryLog.INSTANCE.unsupportedBoundaryScannerType(
							LuceneFastVectorSearchHighlighter.this.getClass().getSimpleName(),
							LuceneFastVectorSearchHighlighter.this.boundaryScannerType
					);
			}
		}

		@Override
		public List<String> highlight(int doc) throws IOException {
			String[] bestFragments = highlighter.getBestFragments(
					highlighter.getFieldQuery( query, leafReaderContext.reader() ),
					leafReaderContext.reader(),
					doc,
					field,
					LuceneFastVectorSearchHighlighter.this.fragmentSize,
					maxNumFragments,
					fragListBuilder,
					fragmentsBuilder,
					preTags,
					postTags,
					LuceneFastVectorSearchHighlighter.this.encoder
			);
			if ( bestFragments != null && bestFragments.length > 0 ) {
				return Arrays.asList( bestFragments );
			}
			else if ( LuceneFastVectorSearchHighlighter.this.noMatchSize > 0 ) {
				SimpleFieldFragList fieldFragList = new SimpleFieldFragList( -1 );
				fieldFragList.add( 0, LuceneFastVectorSearchHighlighter.this.noMatchSize, Collections.emptyList() );

				String[] fragment = noMatchFragments.createFragments(
						leafReaderContext.reader(), doc, field, fieldFragList, 1, preTags, postTags,
						LuceneFastVectorSearchHighlighter.this.encoder
				);
				return fragment == null || fragment.length == 0
						? Collections.emptyList()
						: Collections.singletonList(
								fragment[0] );
			}
			return Collections.emptyList();
		}
	}

	private static class NoMatchFragmentsBuilder extends SimpleFragmentsBuilder {
		public NoMatchFragmentsBuilder(String[] preTags, String[] postTags, BoundaryScanner bs) {
			super( preTags, postTags, bs );
			setDiscreteMultiValueHighlighting( true );
		}

		@Override
		protected List<FieldFragList.WeightedFragInfo> discreteMultiValueHighlighting(
				List<FieldFragList.WeightedFragInfo> fragInfos, Field[] fields) {
			List<FieldFragList.WeightedFragInfo> result = new ArrayList<>();

			for ( Field field : fields ) {
				result.add(
						new FieldFragList.WeightedFragInfo( 0, field.stringValue().length(), Collections.emptyList(), 0.0f ) );
			}

			return result;
		}
	}

	private static class CustomScoreOrderFragmentsBuilder extends ScoreOrderFragmentsBuilder {
		private static final ScoreComparator SCORE_COMPARATOR = new ScoreComparator();

		public CustomScoreOrderFragmentsBuilder(String[] preTags, String[] postTags, BoundaryScanner bs) {
			super( preTags, postTags, bs );
		}

		@Override
		public List<FieldFragList.WeightedFragInfo> getWeightedFragInfoList(List<FieldFragList.WeightedFragInfo> src) {
			Collections.sort( src, SCORE_COMPARATOR );
			return src;
		}
	}

	// TODO: HSEARCH-5160 remove this "custom" class and use the Lucene's constructor for the char[]
	private static class HibernateSearchSimpleBoundaryScanner extends SimpleBoundaryScanner {

		public HibernateSearchSimpleBoundaryScanner(int boundaryMaxScan, char[] boundaryChars) {
			super( boundaryMaxScan, characters( boundaryChars ) );
		}

		private static Character[] characters(char[] chars) {
			Character[] result = new Character[chars.length];
			for ( int i = 0; i < chars.length; i++ ) {
				result[i] = chars[i];
			}
			return result;
		}
	}

}
