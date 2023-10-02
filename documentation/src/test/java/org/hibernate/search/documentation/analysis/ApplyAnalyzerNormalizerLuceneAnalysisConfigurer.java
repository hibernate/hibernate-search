/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.analysis;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class ApplyAnalyzerNormalizerLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( "english" ).custom()
				.tokenizer( "standard" );

		context.analyzer( "my-analyzer" ).custom()
				.tokenizer( "standard" )
				.tokenFilter( "lowercase" );

		context.normalizer( "my-normalizer" ).custom()
				.tokenFilter( "lowercase" );
	}
}
