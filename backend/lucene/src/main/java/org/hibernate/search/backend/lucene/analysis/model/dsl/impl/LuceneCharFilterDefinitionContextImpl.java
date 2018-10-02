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

import org.apache.lucene.analysis.util.CharFilterFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneCharFilterDefinitionContextImpl
		extends LuceneAnalysisComponentDefinitionContextImpl<CharFilterFactory> {

	private final Class<? extends CharFilterFactory> factoryClass;

	LuceneCharFilterDefinitionContextImpl(LuceneCompositeAnalysisDefinitionContext parentContext,
			Class<? extends CharFilterFactory> factoryClass) {
		super( parentContext );
		this.factoryClass = factoryClass;
	}

	@Override
	public CharFilterFactory build(LuceneAnalysisComponentFactory factory) throws IOException {
		return factory.createCharFilterFactory( factoryClass, params );
	}

}
