/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
// tag::include[]
package org.hibernate.search.documentation.mapper.pojo.standalone.gettingstarted.withhsearch.customanalysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class MyLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom() // <1>
				.tokenizer( "standard" ) // <2>
				.tokenFilter( "lowercase" ) // <3>
				.tokenFilter( "snowballPorter" ) // <3>
						.param( "language", "English" ) // <4>
				.tokenFilter( "asciiFolding" );

		context.analyzer( "name" ).custom() // <5>
				.tokenizer( "standard" )
				.tokenFilter( "lowercase" )
				.tokenFilter( "asciiFolding" );
	}
}
// end::include[]
