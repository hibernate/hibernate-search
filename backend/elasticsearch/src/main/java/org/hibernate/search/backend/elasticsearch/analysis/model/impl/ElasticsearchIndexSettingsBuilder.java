/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.impl;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.impl.SimpleElasticsearchAnalysisDefinitionRegistry;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSettingsBuilder {

	private final SimpleElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;

	public ElasticsearchIndexSettingsBuilder(SimpleElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.analysisDefinitionRegistry = new SimpleElasticsearchAnalysisDefinitionRegistry();
	}

	public IndexSettings build() {
		IndexSettings settings = new IndexSettings();

		if ( !analysisDefinitionRegistry.getAnalyzerDefinitions().isEmpty() ) {
			getAnalysis( settings ).setAnalyzers( analysisDefinitionRegistry.getAnalyzerDefinitions() );
		}
		if ( !analysisDefinitionRegistry.getNormalizerDefinitions().isEmpty() ) {
			getAnalysis( settings ).setNormalizers( analysisDefinitionRegistry.getNormalizerDefinitions() );
		}
		if ( !analysisDefinitionRegistry.getTokenizerDefinitions().isEmpty() ) {
			getAnalysis( settings ).setTokenizers( analysisDefinitionRegistry.getTokenizerDefinitions() );
		}
		if ( !analysisDefinitionRegistry.getTokenFilterDefinitions().isEmpty() ) {
			getAnalysis( settings ).setTokenFilters( analysisDefinitionRegistry.getTokenFilterDefinitions() );
		}
		if ( !analysisDefinitionRegistry.getCharFilterDefinitions().isEmpty() ) {
			getAnalysis( settings ).setCharFilters( analysisDefinitionRegistry.getCharFilterDefinitions() );
		}

		return settings;
	}

	/*
	 * Allows lazy initialization of analysis settings
	 */
	private IndexSettings.Analysis getAnalysis(IndexSettings settings) {
		IndexSettings.Analysis analysis = settings.getAnalysis();
		if ( analysis == null ) {
			analysis = new IndexSettings.Analysis();
			settings.setAnalysis( analysis );
		}
		return analysis;
	}

}
