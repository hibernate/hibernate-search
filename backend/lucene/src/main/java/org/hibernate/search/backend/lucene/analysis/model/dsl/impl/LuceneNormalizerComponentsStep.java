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
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerOptionalComponentsStep;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;



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
			return factory.createNormalizer(
					name,
					LuceneAnalysisComponentBuilder.buildAll( charFilters, CharFilterFactory[]::new, factory ),
					LuceneAnalysisComponentBuilder.buildAll( tokenFilters, TokenFilterFactory[]::new, factory )
			);
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToCreateNormalizer( name, e );
		}
	}

}
