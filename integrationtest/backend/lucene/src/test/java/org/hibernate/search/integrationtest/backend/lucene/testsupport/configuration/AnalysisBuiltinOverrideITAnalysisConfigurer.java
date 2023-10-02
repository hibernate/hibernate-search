/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;

public class AnalysisBuiltinOverrideITAnalysisConfigurer implements LuceneAnalysisConfigurer {
	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		overrideAnalyzer( context, AnalyzerNames.DEFAULT );
		overrideAnalyzer( context, AnalyzerNames.STANDARD );
		overrideAnalyzer( context, AnalyzerNames.SIMPLE );
		overrideAnalyzer( context, AnalyzerNames.WHITESPACE );
		overrideAnalyzer( context, AnalyzerNames.STOP );
		overrideAnalyzer( context, AnalyzerNames.KEYWORD );
	}

	private void overrideAnalyzer(LuceneAnalysisConfigurationContext context, String name) {
		context.analyzer( name ).custom()
				.tokenizer( KeywordTokenizerFactory.class )
				.tokenFilter( PatternReplaceFilterFactory.class )
				.param( "pattern", ".*" )
				.param( "replacement", name );
	}
}
