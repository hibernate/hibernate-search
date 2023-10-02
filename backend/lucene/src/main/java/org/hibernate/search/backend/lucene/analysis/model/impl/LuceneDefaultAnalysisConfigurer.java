/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.impl;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public final class LuceneDefaultAnalysisConfigurer implements LuceneAnalysisConfigurer {
	public static final LuceneDefaultAnalysisConfigurer INSTANCE = new LuceneDefaultAnalysisConfigurer();

	private final Analyzer standard = new StandardAnalyzer();
	private final Analyzer simple = new SimpleAnalyzer();
	private final Analyzer whitespace = new WhitespaceAnalyzer();
	private final Analyzer stop = new StopAnalyzer( EnglishAnalyzer.ENGLISH_STOP_WORDS_SET );
	private final Analyzer keyword = new KeywordAnalyzer();

	private LuceneDefaultAnalysisConfigurer() {
	}

	@Override
	public void configure(LuceneAnalysisConfigurationContext context) {
		context.analyzer( AnalyzerNames.DEFAULT ).instance( standard );
		context.analyzer( AnalyzerNames.STANDARD ).instance( standard );
		context.analyzer( AnalyzerNames.SIMPLE ).instance( simple );
		context.analyzer( AnalyzerNames.WHITESPACE ).instance( whitespace );
		context.analyzer( AnalyzerNames.STOP ).instance( stop );
		context.analyzer( AnalyzerNames.KEYWORD ).instance( keyword );
	}
}
