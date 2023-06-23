/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

public class LuceneClassesAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		// @formatter:off
		// tag::include[]
		context.analyzer( "english" ).custom()
				.tokenizer( StandardTokenizerFactory.class )
				.charFilter( HTMLStripCharFilterFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( SnowballPorterFilterFactory.class )
						.param( "language", "English" )
				.tokenFilter( ASCIIFoldingFilterFactory.class );

		context.normalizer( "lowercase" ).custom()
				.tokenFilter( LowerCaseFilterFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class );
		// end::include[]
		// @formatter:on
	}
}
