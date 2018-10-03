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

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisComponentDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCompositeAnalysisDefinitionContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;


abstract class LuceneAnalysisComponentDefinitionContextImpl<T>
		extends DelegatingAnalysisDefinitionContainerContextImpl
		implements LuceneAnalysisComponentDefinitionContext, LuceneAnalysisComponentBuilder<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneCompositeAnalysisDefinitionContext parentContext;

	final Map<String, String> params = new LinkedHashMap<>();

	LuceneAnalysisComponentDefinitionContextImpl(LuceneCompositeAnalysisDefinitionContext parentContext) {
		super( parentContext );
		this.parentContext = parentContext;
	}

	@Override
	public LuceneAnalysisComponentDefinitionContext param(String name, String value) {
		String previous = params.putIfAbsent( name, value );
		if ( previous != null ) {
			throw log.conflictingParameterDefined( name, value, previous );
		}
		return this;
	}

	@Override
	public LuceneAnalysisComponentDefinitionContext charFilter(Class<? extends CharFilterFactory> factory) {
		return parentContext.charFilter( factory );
	}

	@Override
	public LuceneAnalysisComponentDefinitionContext tokenFilter(Class<? extends TokenFilterFactory> factory) {
		return parentContext.tokenFilter( factory );
	}

}
