/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisOptionalComponentsStep;

import org.apache.lucene.analysis.TokenFilterFactory;

class LuceneTokenFilterParametersStep
		extends AbstractLuceneAnalysisComponentParametersStep<TokenFilterFactory> {

	private final Class<? extends TokenFilterFactory> factoryClass;

	LuceneTokenFilterParametersStep(LuceneAnalysisOptionalComponentsStep parentStep,
			Class<? extends TokenFilterFactory> factoryClass) {
		super( parentStep );
		this.factoryClass = factoryClass;
	}

	@Override
	public TokenFilterFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createTokenFilterFactory( factoryClass, params );
	}

}
