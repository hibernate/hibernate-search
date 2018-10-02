/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalyzerFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCharFilterDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneTokenFilterDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.CharFilterDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.NormalizerDef;
import org.hibernate.search.backend.lucene.analysis.model.dsl.impl.annotations.TokenFilterDef;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneNormalizerDefinitionContextImpl implements LuceneNormalizerDefinitionContext {

	private final LuceneAnalysisDefinitionContainerContextImpl parentContext;

	private final String name;

	private final List<LuceneCharFilterDefinitionContextImpl> charFilters = new ArrayList<>();

	private final List<LuceneTokenFilterDefinitionContextImpl> tokenFilters = new ArrayList<>();

	LuceneNormalizerDefinitionContextImpl(LuceneAnalysisDefinitionContainerContextImpl parentContext, String name) {
		this.parentContext = parentContext;
		this.name = name;
	}

	@Override
	public LuceneAnalyzerDefinitionContext analyzer(String name) {
		return parentContext.analyzer( name );
	}

	@Override
	public LuceneNormalizerDefinitionContext normalizer(String name) {
		return parentContext.normalizer( name );
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

	public Analyzer build(LuceneAnalyzerFactory factory) {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( NormalizerDef.class );
		descriptor.setValue( "name", name );

		descriptor.setValue( "charFilters", LuceneAnalysisDefinitionUtils.buildAll( charFilters, CharFilterDef[]::new ) );
		descriptor.setValue( "filters", LuceneAnalysisDefinitionUtils.buildAll( tokenFilters, TokenFilterDef[]::new ) );

		NormalizerDef definition = AnnotationFactory.create( descriptor );
		return factory.createNormalizer( definition );
	}

}
