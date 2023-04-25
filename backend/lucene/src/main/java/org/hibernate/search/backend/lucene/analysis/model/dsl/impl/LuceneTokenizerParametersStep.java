/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisOptionalComponentsStep;

import org.apache.lucene.analysis.TokenizerFactory;

class LuceneTokenizerParametersStep
		extends AbstractLuceneAnalysisComponentParametersStep<TokenizerFactory> {

	private Class<? extends TokenizerFactory> factoryClass;

	LuceneTokenizerParametersStep(LuceneAnalysisOptionalComponentsStep parentStep) {
		super( parentStep );
	}

	public void factory(Class<? extends TokenizerFactory> factoryClass) {
		this.factoryClass = factoryClass;
	}

	@Override
	public TokenizerFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createTokenizerFactory( factoryClass, params );
	}

}
