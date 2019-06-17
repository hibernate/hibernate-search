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

import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCustomAnalyzerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneCustomNormalizerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerDefinitionContext;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionCollector;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionContributor;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;



public class InitialLuceneAnalysisDefinitionContainerContext implements LuceneAnalysisDefinitionContainerContext,
		LuceneAnalysisDefinitionContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneAnalysisComponentFactory factory;

	private final Map<String, LuceneAnalyzerBuilder> analyzers = new LinkedHashMap<>();

	private final Map<String, LuceneAnalyzerBuilder> normalizers = new LinkedHashMap<>();

	public InitialLuceneAnalysisDefinitionContainerContext(LuceneAnalysisComponentFactory factory) {
		this.factory = factory;
	}

	@Override
	public LuceneAnalyzerDefinitionContext analyzer(String name) {
		return new LuceneAnalyzerDefinitionContext() {
			@Override
			public LuceneCustomAnalyzerDefinitionContext custom() {
				LuceneCustomAnalyzerDefinitionContextImpl definition = new LuceneCustomAnalyzerDefinitionContextImpl(
						InitialLuceneAnalysisDefinitionContainerContext.this, name
				);
				addAnalyzer( name, definition );
				return definition;
			}

			@Override
			public LuceneAnalysisDefinitionContainerContext instance(Analyzer instance) {
				LuceneAnalyzerInstanceContext definition = new LuceneAnalyzerInstanceContext( instance );
				addAnalyzer( name, definition );
				return InitialLuceneAnalysisDefinitionContainerContext.this;
			}
		};
	}

	@Override
	public LuceneNormalizerDefinitionContext normalizer(String name) {
		return new LuceneNormalizerDefinitionContext() {
			@Override
			public LuceneCustomNormalizerDefinitionContext custom() {
				LuceneCustomNormalizerDefinitionContextImpl definition = new LuceneCustomNormalizerDefinitionContextImpl(
						InitialLuceneAnalysisDefinitionContainerContext.this, name
				);
				addNormalizer( name, definition );
				return definition;
			}

			@Override
			public LuceneAnalysisDefinitionContainerContext instance(Analyzer instance) {
				LuceneNormalizerInstanceContext definition = new LuceneNormalizerInstanceContext( name, instance );
				addNormalizer( name, definition );
				return InitialLuceneAnalysisDefinitionContainerContext.this;
			}
		};
	}

	@Override
	public void contribute(LuceneAnalysisDefinitionCollector collector) {
		for ( Map.Entry<String, LuceneAnalyzerBuilder> entry : analyzers.entrySet() ) {
			collector.collectAnalyzer( entry.getKey(), entry.getValue().build( factory ) );
		}
		for ( Map.Entry<String, LuceneAnalyzerBuilder> entry : normalizers.entrySet() ) {
			collector.collectNormalizer( entry.getKey(), entry.getValue().build( factory ) );
		}
	}

	private void addAnalyzer(String name, LuceneAnalyzerBuilder definition) {
		LuceneAnalysisComponentBuilder<Analyzer> existing = analyzers.putIfAbsent( name, definition );
		if ( existing != null ) {
			throw log.analyzerDefinitionNamingConflict( name );
		}
	}

	private void addNormalizer(String name, LuceneAnalyzerBuilder definition) {
		LuceneAnalysisComponentBuilder<Analyzer> existing = normalizers.putIfAbsent( name, definition );
		if ( existing != null ) {
			throw log.normalizerDefinitionNamingConflict( name );
		}
	}

}
