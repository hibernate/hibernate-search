/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.gettingstarted.withhsearch.withanalysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;

public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisDefinitionContainerContext context) {
		context.analyzer( "english" ).custom() // <1>
				.tokenizer( StandardTokenizerFactory.class ) // <2>
				.tokenFilter( ASCIIFoldingFilterFactory.class ) // <3>
				.tokenFilter( LowerCaseFilterFactory.class ) // <3>
				.tokenFilter( SnowballPorterFilterFactory.class ) // <3>
						.param( "language", "English" ); // <4>

		context.analyzer( "name" ).custom() // <5>
				.tokenizer( StandardTokenizerFactory.class )
				.tokenFilter( ASCIIFoldingFilterFactory.class )
				.tokenFilter( LowerCaseFilterFactory.class );
	}
}
// end::include[]
