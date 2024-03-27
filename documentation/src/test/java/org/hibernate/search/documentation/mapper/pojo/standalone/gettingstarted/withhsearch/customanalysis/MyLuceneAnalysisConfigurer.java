/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
// @formatter:off
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
// @formatter:on
