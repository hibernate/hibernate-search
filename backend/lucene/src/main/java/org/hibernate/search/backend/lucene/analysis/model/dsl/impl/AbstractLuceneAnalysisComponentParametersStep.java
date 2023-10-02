/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentParametersStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisOptionalComponentsStep;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;

abstract class AbstractLuceneAnalysisComponentParametersStep<T>
		implements LuceneAnalysisComponentParametersStep, LuceneAnalysisComponentBuilder<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneAnalysisOptionalComponentsStep parentStep;

	final Map<String, String> params = new LinkedHashMap<>();

	AbstractLuceneAnalysisComponentParametersStep(LuceneAnalysisOptionalComponentsStep parentStep) {
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
	public LuceneAnalysisComponentParametersStep charFilter(String factoryName) {
		return parentStep.charFilter( factoryName );
	}

	@Override
	public LuceneAnalysisComponentParametersStep charFilter(Class<? extends CharFilterFactory> factoryType) {
		return parentStep.charFilter( factoryType );
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(String factoryName) {
		return parentStep.tokenFilter( factoryName );
	}

	@Override
	public LuceneAnalysisComponentParametersStep tokenFilter(Class<? extends TokenFilterFactory> factoryType) {
		return parentStep.tokenFilter( factoryType );
	}

}
