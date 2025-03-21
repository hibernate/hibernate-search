/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerOptionalComponentsStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTokenizerStep;
import org.hibernate.search.backend.lucene.logging.impl.AnalysisLog;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenizerFactory;

class LuceneAnalyzerComponentsStep
		implements LuceneAnalyzerTokenizerStep, LuceneAnalyzerOptionalComponentsStep,
		LuceneAnalyzerBuilder {

	private final String name;

	private final LuceneTokenizerParametersStep tokenizer;

	private final List<LuceneCharFilterParametersStep> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterParametersStep> tokenFilters = new ArrayList<>();

	LuceneAnalyzerComponentsStep(String name) {
		this.tokenizer = new LuceneTokenizerParametersStep( this );
		this.name = name;
	}

	@Override
	public LuceneAnalyzerOptionalComponentsStep tokenizer(String factoryName) {
		return tokenizer( TokenizerFactory.lookupClass( factoryName ) );
	}

	@Override
	public LuceneAnalyzerOptionalComponentsStep tokenizer(Class<? extends TokenizerFactory> factoryType) {
		tokenizer.factory( factoryType );
		return this;
	}

	@Override
	public LuceneAnalyzerOptionalComponentsStep param(String name, String value) {
		tokenizer.param( name, value );
		return this;
	}

	@Override
	public LuceneAnalysisComponentParametersStep charFilter(String factoryName) {
		return charFilter( CharFilterFactory.lookupClass( factoryName ) );
	}

	@Override
	public LuceneAnalysisComponentParametersStep charFilter(Class<? extends CharFilterFactory> factoryType) {
		LuceneCharFilterParametersStep filter = new LuceneCharFilterParametersStep( this, factoryType );
		charFilters.add( filter );
		return filter;
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(String factoryName) {
		return tokenFilter( TokenFilterFactory.lookupClass( factoryName ) );
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(Class<? extends TokenFilterFactory> factoryType) {
		LuceneTokenFilterParametersStep filter = new LuceneTokenFilterParametersStep( this, factoryType );
		tokenFilters.add( filter );
		return filter;
	}

	@Override
	public Analyzer build(LuceneAnalysisComponentFactory factory) {
		try {
			return factory.createAnalyzer(
					tokenizer.build( factory ),
					LuceneAnalysisComponentBuilder.buildAll( charFilters, CharFilterFactory[]::new, factory ),
					LuceneAnalysisComponentBuilder.buildAll( tokenFilters, TokenFilterFactory[]::new, factory )
			);
		}
		catch (IOException | RuntimeException e) {
			throw AnalysisLog.INSTANCE.unableToCreateAnalyzer( name, e.getMessage(), e );
		}
	}

}
