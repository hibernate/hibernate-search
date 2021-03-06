/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;


public class LowLevelIndexMetadataBuilder {

	private final ElasticsearchIndexMetadataSyntax syntax;
	private final IndexNames indexNames;
	private ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private IndexSettings customIndexSettings;
	private RootTypeMapping mapping;

	public LowLevelIndexMetadataBuilder(ElasticsearchIndexMetadataSyntax syntax, IndexNames indexNames) {
		this.syntax = syntax;
		this.indexNames = indexNames;
	}

	public void setAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	public void setCustomIndexSettings(IndexSettings customIndexSettings) {
		this.customIndexSettings = customIndexSettings;
	}

	public void setMapping(RootTypeMapping mapping) {
		this.mapping = mapping;
	}

	public IndexMetadata build() {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setAliases( buildAliases() );

		indexMetadata.setSettings( buildSettings() );

		indexMetadata.setMapping( mapping );
		return indexMetadata;
	}

	private Map<String, IndexAliasDefinition> buildAliases() {
		Map<String, IndexAliasDefinition> aliases = new LinkedHashMap<>();
		if ( indexNames.writeIsAlias() ) {
			aliases.put( indexNames.write().original, syntax.createWriteAliasDefinition() );
		}
		if ( indexNames.readIsAlias() ) {
			aliases.put( indexNames.read().original, syntax.createReadAliasDefinition() );
		}
		return aliases;
	}

	private IndexSettings buildSettings() {
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

		// if customSettings are present, merge them with the ones created by Search
		settings.merge( customIndexSettings );

		return settings;
	}

	/*
	 * Allows lazy initialization of analysis settings
	 */
	private Analysis getAnalysis(IndexSettings settings) {
		Analysis analysis = settings.getAnalysis();
		if ( analysis == null ) {
			analysis = new Analysis();
			settings.setAnalysis( analysis );
		}
		return analysis;
	}

}
