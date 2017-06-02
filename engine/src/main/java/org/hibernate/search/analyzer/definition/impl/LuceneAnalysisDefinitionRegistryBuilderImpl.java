/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneNormalizerDefinitionContext;
import org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionRegistryBuilder;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class LuceneAnalysisDefinitionRegistryBuilderImpl implements LuceneAnalysisDefinitionRegistryBuilder {

	private static final Log LOG = LoggerFactory.make();

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

	public SimpleLuceneAnalysisDefinitionRegistry build() {
		SimpleLuceneAnalysisDefinitionRegistry registry = new SimpleLuceneAnalysisDefinitionRegistry();
		for ( Map.Entry<String, LuceneAnalyzerDefinitionContextImpl> entry : analyzerDefinitions.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue().build() );
		}
		for ( Map.Entry<String, LuceneNormalizerDefinitionContextImpl> entry : normalizerDefinitions.entrySet() ) {
			registry.register( entry.getKey(), entry.getValue().build() );
		}
		return registry;
	}

}
