/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCompositeAnalysisDefinitionContext;

import org.apache.lucene.analysis.util.TokenFilterFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneTokenFilterDefinitionContextImpl
		extends LuceneAnalysisComponentDefinitionContextImpl<TokenFilterFactory> {

	private final Class<? extends TokenFilterFactory> factoryClass;

	LuceneTokenFilterDefinitionContextImpl(LuceneCompositeAnalysisDefinitionContext parentContext,
			Class<? extends TokenFilterFactory> factoryClass) {
		super( parentContext );
		this.factoryClass = factoryClass;
	}

	@Override
	public TokenFilterFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createTokenFilterFactory( factoryClass, params );
	}

}
