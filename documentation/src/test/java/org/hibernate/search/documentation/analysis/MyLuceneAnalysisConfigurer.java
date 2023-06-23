/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// @formatter:off
// tag::include[]
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom() // <1>
				.tokenizer( "standard" ) // <2>
				.charFilter( "htmlStrip" ) // <3>
				.tokenFilter( "lowercase" ) // <4>
				.tokenFilter( "snowballPorter" ) // <4>
						.param( "language", "English" ) // <5>
				.tokenFilter( "asciiFolding" );

		context.normalizer( "lowercase" ).custom() // <6>
				.tokenFilter( "lowercase" )
				.tokenFilter( "asciiFolding" );

		context.analyzer( "french" ).custom() // <7>
				.tokenizer( "standard" )
				.charFilter( "htmlStrip" )
				.tokenFilter( "lowercase" )
				.tokenFilter( "snowballPorter" )
						.param( "language", "French" )
				.tokenFilter( "asciiFolding" );
	}
}
// end::include[]
// @formatter:on
