/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisOptionalComponentsStep;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;


abstract class AbstractLuceneAnalysisComponentParametersStep<T>
		extends DelegatingAnalysisDefinitionContainerContext
		implements LuceneAnalysisComponentParametersStep, LuceneAnalysisComponentBuilder<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneAnalysisOptionalComponentsStep parentStep;

	final Map<String, String> params = new LinkedHashMap<>();

	AbstractLuceneAnalysisComponentParametersStep(LuceneAnalysisOptionalComponentsStep parentStep) {
		super( parentStep );
		this.parentStep = parentStep;
	}

	@Override
	public LuceneAnalysisComponentParametersStep param(String name, String value) {
		String previous = params.putIfAbsent( name, value );
		if ( previous != null ) {
			throw log.analysisComponentParameterConflict( name, previous, value );
		}
		return this;
	}

	@Override
	public LuceneAnalysisComponentParametersStep charFilter(Class<? extends CharFilterFactory> factory) {
		return parentStep.charFilter( factory );
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(Class<? extends TokenFilterFactory> factory) {
		return parentStep.tokenFilter( factory );
	}

}
