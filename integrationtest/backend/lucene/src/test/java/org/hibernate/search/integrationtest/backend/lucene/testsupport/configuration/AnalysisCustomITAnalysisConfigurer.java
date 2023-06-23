/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.analysis.AnalysisCustomIT;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;

public class AnalysisCustomITAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_NOOP.name ).custom();
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_LOWERCASE.name ).custom()
				.tokenFilter( LowerCaseFilterFactory.class );
		context.normalizer( AnalysisCustomIT.AnalysisDefinitions.NORMALIZER_PATTERN_REPLACING.name ).custom()
				.charFilter( PatternReplaceCharFilterFactory.class )
				.param( "pattern", "\\s+" )
				.param( "replacement", "," );

		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_NOOP.name ).custom()
				.tokenizer( KeywordTokenizerFactory.class );
		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ).custom()
				.tokenizer( WhitespaceTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class );

		context.analyzer( AnalysisCustomIT.AnalysisDefinitions.ANALYZER_PATTERNS_STOPWORD.name ).custom()
				.tokenizer( PatternTokenizerFactory.class )
				.param( "pattern", "," )
				.charFilter( PatternReplaceCharFilterFactory.class )
				.param( "pattern", "\\s+" )
				.param( "replacement", "," )
				.tokenFilter( StopFilterFactory.class )
				.param( "words", "/analysis/AnalysisCustomIT/stopwords.txt" );
	}
}
