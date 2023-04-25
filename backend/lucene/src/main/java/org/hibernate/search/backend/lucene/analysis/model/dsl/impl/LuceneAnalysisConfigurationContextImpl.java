/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisComponentFactory;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTokenizerStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneAnalyzerTypeStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerOptionalComponentsStep;
import org.hibernate.search.backend.lucene.analysis.model.dsl.LuceneNormalizerTypeStep;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionCollector;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionContributor;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.search.similarities.Similarity;

public class LuceneAnalysisConfigurationContextImpl
		implements LuceneAnalysisConfigurationContext, LuceneAnalysisDefinitionContributor {

	private final LuceneAnalysisComponentFactory factory;

	private Similarity similarity;

	private final Map<String, LuceneAnalyzerBuilder> analyzers = new LinkedHashMap<>();

	private final Map<String, LuceneAnalyzerBuilder> normalizers = new LinkedHashMap<>();

	public LuceneAnalysisConfigurationContextImpl(LuceneAnalysisComponentFactory factory) {
		this.factory = factory;
	}

	@Override
	public LuceneAnalyzerTypeStep analyzer(String name) {
		return new LuceneAnalyzerTypeStep() {
			@Override
			public LuceneAnalyzerTokenizerStep custom() {
				LuceneAnalyzerComponentsStep definition = new LuceneAnalyzerComponentsStep( name );
				addAnalyzer( name, definition );
				return definition;
			}

			@Override
			public LuceneAnalysisConfigurationContext instance(Analyzer instance) {
				LuceneAnalyzerInstanceBuilder definition = new LuceneAnalyzerInstanceBuilder( instance );
				addAnalyzer( name, definition );
				return LuceneAnalysisConfigurationContextImpl.this;
			}
		};
	}

	@Override
	public LuceneNormalizerTypeStep normalizer(String name) {
		return new LuceneNormalizerTypeStep() {
			@Override
			public LuceneNormalizerOptionalComponentsStep custom() {
				LuceneNormalizerComponentsStep definition = new LuceneNormalizerComponentsStep( name );
				addNormalizer( name, definition );
				return definition;
			}

			@Override
			public LuceneAnalysisConfigurationContext instance(Analyzer instance) {
				LuceneNormalizerInstanceBuilder definition = new LuceneNormalizerInstanceBuilder( name, instance );
				addNormalizer( name, definition );
				return LuceneAnalysisConfigurationContextImpl.this;
			}
		};
	}

	@Override
	public void similarity(Similarity similarity) {
		this.similarity = similarity;
	}

	@Override
	public Set<String> availableTokenizers() {
		return TokenizerFactory.availableTokenizers();
	}

	@Override
	public Set<String> availableCharFilters() {
		return CharFilterFactory.availableCharFilters();
	}

	@Override
	public Set<String> availableTokenFilters() {
		return TokenFilterFactory.availableTokenFilters();
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

	@Override
	public Optional<Similarity> getSimilarity() {
		return Optional.ofNullable( similarity );
	}

	private void addAnalyzer(String name, LuceneAnalyzerBuilder definition) {
		// Override if existing
		analyzers.put( name, definition );
	}

	private void addNormalizer(String name, LuceneAnalyzerBuilder definition) {
		// Override if existing
		normalizers.putIfAbsent( name, definition );
	}

}
