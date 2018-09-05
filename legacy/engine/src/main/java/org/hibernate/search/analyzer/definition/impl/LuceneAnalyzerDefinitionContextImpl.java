/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionWithTokenizerContext;
import org.hibernate.search.analyzer.definition.LuceneCharFilterDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneNormalizerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneTokenFilterDefinitionContext;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.TokenFilterDef;


/**
 * @author Yoann Rodiere
 */
public class LuceneAnalyzerDefinitionContextImpl
		implements LuceneAnalyzerDefinitionContext, LuceneAnalyzerDefinitionWithTokenizerContext {

	private final LuceneAnalysisDefinitionRegistryBuilderImpl registry;

	private final String name;

	private final LuceneTokenizerDefinitionContextImpl tokenizer = new LuceneTokenizerDefinitionContextImpl();

	private final List<LuceneCharFilterDefinitionContextImpl> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterDefinitionContextImpl> tokenFilters = new ArrayList<>();

	public LuceneAnalyzerDefinitionContextImpl(LuceneAnalysisDefinitionRegistryBuilderImpl registry, String name) {
		this.registry = registry;
		this.name = name;
	}

	@Override
	public LuceneAnalyzerDefinitionContext analyzer(String name) {
		return registry.analyzer( name );
	}

	@Override
	public LuceneNormalizerDefinitionContext normalizer(String name) {
		return registry.normalizer( name );
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
	public LuceneCharFilterDefinitionContext charFilter(Class<? extends CharFilterFactory> factory) {
		LuceneCharFilterDefinitionContextImpl filter = new LuceneCharFilterDefinitionContextImpl( this, factory );
		charFilters.add( filter );
		return filter;
	}

	@Override
	public LuceneTokenFilterDefinitionContext tokenFilter(Class<? extends TokenFilterFactory> factory) {
		LuceneTokenFilterDefinitionContextImpl filter = new LuceneTokenFilterDefinitionContextImpl( this, factory );
		tokenFilters.add( filter );
		return filter;
	}

	public AnalyzerDef build() {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( AnalyzerDef.class );
		descriptor.setValue( "name", name );
		descriptor.setValue( "tokenizer", tokenizer.build() );

		descriptor.setValue( "charFilters", LuceneAnalysisDefinitionUtils.buildAll( charFilters, CharFilterDef[]::new ) );
		descriptor.setValue( "filters", LuceneAnalysisDefinitionUtils.buildAll( tokenFilters, TokenFilterDef[]::new ) );
		return AnnotationFactory.create( descriptor );
	}

}
