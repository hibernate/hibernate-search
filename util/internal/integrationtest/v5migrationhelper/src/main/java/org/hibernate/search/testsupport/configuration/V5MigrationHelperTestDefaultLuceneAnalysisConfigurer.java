/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.testsupport.AnalysisNames;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.search.similarities.ClassicSimilarity;

public class V5MigrationHelperTestDefaultLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.similarity( new ClassicSimilarity() );

		context.normalizer( AnalysisNames.NORMALIZER_LOWERCASE ).custom()
				.tokenFilter( LowerCaseFilterFactory.class );
		context.normalizer( AnalysisNames.NORMALIZER_LOWERCASE_ASCIIFOLDING ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.analyzer( AnalysisNames.ANALYZER_WHITESPACE ).custom()
				.tokenizer( WhitespaceTokenizerFactory.class );
		context.analyzer( AnalysisNames.ANALYZER_WHITESPACE_LOWERCASE_ASCIIFOLDING ).custom()
				.tokenizer( WhitespaceTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
		context.analyzer( AnalysisNames.ANALYZER_STANDARD ).instance( new StandardAnalyzer() );
		context.analyzer( AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				// Skipping StandardFilter: it was removed in Lucene 7.5/8 because it was a no-op.
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( StopFilterFactory.class );
		context.analyzer( AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_NGRAM_3 ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				// Skipping StandardFilter: it was removed in Lucene 7.5/8 because it was a no-op.
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( StopFilterFactory.class )
				.tokenFilter( NGramFilterFactory.class )
				.param( "minGramSize", "3" )
				.param( "maxGramSize", "3" );
		context.analyzer( AnalysisNames.ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_STEMMER_ENGLISH ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				// Skipping StandardFilter: it was removed in Lucene 7.5/8 because it was a no-op.
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( StopFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" );
		context.analyzer( AnalysisNames.ANALYZER_STANDARD_HTML_STRIP_ESCAPED_LOWERCASE ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.charFilter( HTMLStripCharFilterFactory.class )
				.param( "escapedTags", "escaped" )
				.tokenFilter( LowerCaseFilterFactory.class );
	}
}
