/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneCharFilterDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneCompositeAnalysisDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneNormalizerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneTokenFilterDefinitionContext;
import org.hibernate.search.annotations.CharFilterDef;


/**
 * @author Yoann Rodiere
 */
public class LuceneCharFilterDefinitionContextImpl
		implements LuceneCharFilterDefinitionContext, LuceneAnalysisDefinitionBuilder<CharFilterDef> {

	private final LuceneCompositeAnalysisDefinitionContext parentContext;

	private final Class<? extends CharFilterFactory> factoryClass;

	private final ParametersBuilder params = new ParametersBuilder();

	public LuceneCharFilterDefinitionContextImpl(
			LuceneCompositeAnalysisDefinitionContext parentContext,
			Class<? extends CharFilterFactory> factoryClass) {
		this.parentContext = parentContext;
		this.factoryClass = factoryClass;
	}

	@Override
	public LuceneCharFilterDefinitionContext param(String name, String value) {
		params.put( name, value );
		return this;
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
		return parentContext.charFilter( factory );
	}

	@Override
	public LuceneTokenFilterDefinitionContext tokenFilter(Class<? extends TokenFilterFactory> factory) {
		return parentContext.tokenFilter( factory );
	}

	@Override
	public CharFilterDef build() {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( CharFilterDef.class );
		descriptor.setValue( "factory", factoryClass );
		descriptor.setValue( "params", params.build() );
		return AnnotationFactory.create( descriptor );
	}

}
