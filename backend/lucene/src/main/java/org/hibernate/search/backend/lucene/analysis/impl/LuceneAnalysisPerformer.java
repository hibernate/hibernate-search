/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public class LuceneAnalysisPerformer {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneBackend backend;

	public LuceneAnalysisPerformer(LuceneBackend backend) {
		this.backend = backend;
	}

	public List<? extends AnalysisToken> analyze(String analyzerName, String terms) {
		Analyzer analyzer = backend.analyzer( analyzerName )
				.orElseThrow( () -> log.noSuchAnalyzer( analyzerName ) );

		return analyze( analyzerName, analyzer, terms );
	}

	public AnalysisToken normalize(String normalizerName, String terms) {
		Analyzer analyzer = backend.normalizer( normalizerName )
				.orElseThrow( () -> log.noSuchNormalizer( normalizerName ) );

		List<LuceneAnalysisToken> tokens = analyze( normalizerName, analyzer, terms );
		if ( tokens.size() != 1 ) {
			throw new AssertionFailure( "Applying an normalizer to a string should have produced a single token." +
					" Instead applying " + normalizerName + " to '" + terms + "' produced: " + tokens );
		}
		return tokens.get( 0 );
	}

	private static List<LuceneAnalysisToken> analyze(String analyzerName, Analyzer analyzer, String string) {
		List<LuceneAnalysisToken> tokens = new ArrayList<>();
		try ( TokenStream tokenStream = analyzer.tokenStream( "", string ) ) {
			CharTermAttribute termAttribute = tokenStream.addAttribute( CharTermAttribute.class );
			OffsetAttribute offsetAttribute = tokenStream.addAttribute( OffsetAttribute.class );
			TypeAttribute typeAttribute = tokenStream.addAttribute( TypeAttribute.class );

			tokenStream.reset();

			while ( tokenStream.incrementToken() ) {
				tokens.add( new LuceneAnalysisToken(
						termAttribute.toString(),
						offsetAttribute.startOffset(),
						offsetAttribute.endOffset(),
						typeAttribute.type()
				) );
			}
		}
		catch (IOException e) {
			throw log.unableToPerformAnalysisOperation( analyzerName, string, e.getMessage(), e );
		}
		return tokens;
	}

	private static class LuceneAnalysisToken implements AnalysisToken {

		private final String term;
		private final int startOffset;
		private final int endOffset;
		private final String type;

		private LuceneAnalysisToken(String term, int startOffset, int endOffset, String type) {
			this.term = term;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
			this.type = type;
		}

		@Override
		public String term() {
			return term;
		}

		@Override
		public int startOffset() {
			return startOffset;
		}

		@Override
		public int endOffset() {
			return endOffset;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public String toString() {
			return "AnalysisToken{" +
					"value='" + term + '\'' +
					", startOffset=" + startOffset +
					", endOffset=" + endOffset +
					", type='" + type + '\'' +
					'}';
		}
	}
}
