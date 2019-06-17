/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCustomAnalysisDefinitionContext;

import org.apache.lucene.analysis.util.TokenFilterFactory;



public class LuceneTokenFilterDefinitionContext
		extends AbstractLuceneAnalysisComponentDefinitionContext<TokenFilterFactory> {

	private final Class<? extends TokenFilterFactory> factoryClass;

	LuceneTokenFilterDefinitionContext(LuceneCustomAnalysisDefinitionContext parentContext,
			Class<? extends TokenFilterFactory> factoryClass) {
		super( parentContext );
		this.factoryClass = factoryClass;
	}

	@Override
	public TokenFilterFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createTokenFilterFactory( factoryClass, params );
	}

}
