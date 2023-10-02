/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerOptionalComponentsStep;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;

class LuceneNormalizerComponentsStep
		implements LuceneNormalizerOptionalComponentsStep, LuceneAnalyzerBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final List<LuceneCharFilterParametersStep> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterParametersStep> tokenFilters = new ArrayList<>();

	LuceneNormalizerComponentsStep(String name) {
		this.name = name;
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
			return factory.createNormalizer(
					name,
					LuceneAnalysisComponentBuilder.buildAll( charFilters, CharFilterFactory[]::new, factory ),
					LuceneAnalysisComponentBuilder.buildAll( tokenFilters, TokenFilterFactory[]::new, factory )
			);
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToCreateNormalizer( name, e.getMessage(), e );
		}
	}

}
