/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class DefaultITAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
				.instance( new StandardAnalyzer( EnglishAnalyzer.ENGLISH_STOP_WORDS_SET ) );

		context.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ).custom()
				.tokenFilter( LowerCaseFilterFactory.class );

		context.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ).custom()
				.tokenizer( WhitespaceTokenizerFactory.class );

		context.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ).custom()
				.tokenizer( WhitespaceTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class );

		context.analyzer( DefaultAnalysisDefinitions.ANALYZER_NGRAM.name ).custom()
				.tokenizer( NGramTokenizerFactory.class )
				.param( "minGramSize", "5" )
				.param( "maxGramSize", "6" );
	}
}
