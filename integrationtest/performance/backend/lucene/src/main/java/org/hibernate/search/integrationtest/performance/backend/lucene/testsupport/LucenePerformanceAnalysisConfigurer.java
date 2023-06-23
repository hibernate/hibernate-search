/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.lucene.testsupport;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.analysis.Analyzers;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

public class LucenePerformanceAnalysisConfigurer implements LuceneAnalysisConfigurer {

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( Analyzers.ANALYZER_ENGLISH ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
				.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.normalizer( Analyzers.NORMALIZER_ENGLISH ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
	}

}
