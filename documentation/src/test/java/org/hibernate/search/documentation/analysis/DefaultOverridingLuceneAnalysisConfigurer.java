/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
// @formatter:off
// tag::include[]
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

public class DefaultOverridingLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( AnalyzerNames.DEFAULT ) // <1>
				.custom() // <2>
				.tokenizer( "standard" )
				.tokenFilter( "lowercase" )
				.tokenFilter( "snowballPorter" )
						.param( "language", "French" )
				.tokenFilter( "asciiFolding" );
	}
}
// end::include[]
// @formatter:on
