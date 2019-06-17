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
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCustomAnalyzerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerDefinitionWithTokenizerContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;



public class LuceneCustomAnalyzerDefinitionContextImpl
		extends DelegatingAnalysisDefinitionContainerContext
		implements LuceneCustomAnalyzerDefinitionContext, LuceneAnalyzerDefinitionWithTokenizerContext,
		LuceneAnalyzerBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final LuceneTokenizerDefinitionContext tokenizer;

	private final List<LuceneCharFilterDefinitionContext> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterDefinitionContext> tokenFilters = new ArrayList<>();

	LuceneCustomAnalyzerDefinitionContextImpl(InitialLuceneAnalysisDefinitionContainerContext parentContext, String name) {
		super( parentContext );
		this.tokenizer = new LuceneTokenizerDefinitionContext( this );
		this.name = name;
	}

	@Override
	public LuceneAnalyzerDefinitionWithTokenizerContext tokenizer(Class<? extends TokenizerFactory> factory) {
		tokenizer.factory( factory );
		return this;
	}

	@Override
	public LuceneAnalyzerDefinitionWithTokenizerContext param(String name, String value) {
		tokenizer.param( name, value );
		return this;
	}

	@Override
	public LuceneAnalysisComponentDefinitionContext charFilter(Class<? extends CharFilterFactory> factory) {
		LuceneCharFilterDefinitionContext filter = new LuceneCharFilterDefinitionContext( this, factory );
		charFilters.add( filter );
		return filter;
	}

	@Override
	public LuceneAnalysisComponentDefinitionContext tokenFilter(Class<? extends TokenFilterFactory> factory) {
		LuceneTokenFilterDefinitionContext filter = new LuceneTokenFilterDefinitionContext( this, factory );
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
