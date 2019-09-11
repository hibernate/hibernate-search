/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

class LuceneSimpleMappingAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
						.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.analyzer( "name" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.analyzer( "autocomplete_indexing" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
						.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class )
				.tokenFilter( EdgeNGramFilterFactory.class )
						.param( "minGramSize", "3" )
						.param( "maxGramSize", "7" );

		// Same as "autocomplete-indexing", but without the edge-ngram filter
		context.analyzer( "autocomplete_query" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
						.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		// Normalizers

		context.normalizer( "english" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.normalizer( "name" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
	}
}
