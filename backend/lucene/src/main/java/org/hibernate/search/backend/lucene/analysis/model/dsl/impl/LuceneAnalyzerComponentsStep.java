/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTokenizerStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerOptionalComponentsStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;



class LuceneAnalyzerComponentsStep
		extends DelegatingAnalysisDefinitionContainerContext
		implements LuceneAnalyzerTokenizerStep, LuceneAnalyzerOptionalComponentsStep,
		LuceneAnalyzerBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final LuceneTokenizerParametersStep tokenizer;

	private final List<LuceneCharFilterParametersStep> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterParametersStep> tokenFilters = new ArrayList<>();

	LuceneAnalyzerComponentsStep(LuceneAnalysisConfigurationContextImpl parentContext, String name) {
		super( parentContext );
		this.tokenizer = new LuceneTokenizerParametersStep( this );
		this.name = name;
	}

	@Override
	public LuceneAnalyzerOptionalComponentsStep tokenizer(Class<? extends TokenizerFactory> factory) {
		tokenizer.factory( factory );
		return this;
	}

	@Override
	public LuceneAnalyzerOptionalComponentsStep param(String name, String value) {
		tokenizer.param( name, value );
		return this;
	}

	@Override
	public LuceneAnalysisComponentParametersStep charFilter(Class<? extends CharFilterFactory> factory) {
		LuceneCharFilterParametersStep filter = new LuceneCharFilterParametersStep( this, factory );
		charFilters.add( filter );
		return filter;
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(Class<? extends TokenFilterFactory> factory) {
		LuceneTokenFilterParametersStep filter = new LuceneTokenFilterParametersStep( this, factory );
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
			throw log.unableToCreateAnalyzer( name, e );
		}
	}

}
