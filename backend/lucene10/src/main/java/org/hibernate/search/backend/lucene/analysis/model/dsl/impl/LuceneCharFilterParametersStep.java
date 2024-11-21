/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisOptionalComponentsStep;

import org.apache.lucene.analysis.CharFilterFactory;

class LuceneCharFilterParametersStep
		extends AbstractLuceneAnalysisComponentParametersStep<CharFilterFactory> {

	private final Class<? extends CharFilterFactory> factoryClass;

	LuceneCharFilterParametersStep(LuceneAnalysisOptionalComponentsStep parentStep,
			Class<? extends CharFilterFactory> factoryClass) {
		super( parentStep );
		this.factoryClass = factoryClass;
	}

	@Override
	public CharFilterFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createCharFilterFactory( factoryClass, params );
	}

}
