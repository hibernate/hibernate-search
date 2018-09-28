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

import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionCollector;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionContributor;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.impl.common.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneAnalysisDefinitionContainerContextImpl implements LuceneAnalysisDefinitionContainerContext,
		LuceneAnalysisDefinitionContributor {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Map<String, LuceneAnalyzerDefinitionContextImpl> analyzerDefinitions = new LinkedHashMap<>();

	private Map<String, LuceneNormalizerDefinitionContextImpl> normalizerDefinitions = new LinkedHashMap<>();

	@Override
	public LuceneAnalyzerDefinitionContext analyzer(String name) {
		LuceneAnalyzerDefinitionContextImpl definition = new LuceneAnalyzerDefinitionContextImpl( this, name );
		LuceneAnalyzerDefinitionContextImpl existing = analyzerDefinitions.putIfAbsent( name, definition );
		if ( existing != null ) {
			throw LOG.analyzerDefinitionNamingConflict( name );
		}
		return definition;
	}

	@Override
	public LuceneNormalizerDefinitionContext normalizer(String name) {
		LuceneNormalizerDefinitionContextImpl definition = new LuceneNormalizerDefinitionContextImpl( this, name );
		LuceneNormalizerDefinitionContextImpl existing = normalizerDefinitions.putIfAbsent( name, definition );
		if ( existing != null ) {
			throw LOG.normalizerDefinitionNamingConflict( name );
		}
		return definition;
	}

	@Override
	public void contribute(LuceneAnalysisDefinitionCollector collector) {
		for ( Map.Entry<String, LuceneAnalyzerDefinitionContextImpl> entry : analyzerDefinitions.entrySet() ) {
			collector.collect( entry.getKey(), entry.getValue().build() );
		}
		for ( Map.Entry<String, LuceneNormalizerDefinitionContextImpl> entry : normalizerDefinitions.entrySet() ) {
			collector.collect( entry.getKey(), entry.getValue().build() );
		}
	}

}
