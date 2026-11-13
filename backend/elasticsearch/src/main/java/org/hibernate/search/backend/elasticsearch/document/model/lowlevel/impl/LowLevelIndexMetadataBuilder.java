package org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonUtils;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.PropertyMappingIndexSettingsContributor;
import org.hibernate.search.backend.elasticsearch.lowlevel.syntax.metadata.impl.ElasticsearchIndexMetadataSyntax;

public class LowLevelIndexMetadataBuilder {

	private final JsonProvider jsonProvider;
	private final ElasticsearchIndexMetadataSyntax syntax;
	private final IndexNames indexNames;
	private PropertyMappingIndexSettingsContributor propertyMappingIndexSettingsContributor;
	private ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private IndexSettings customIndexSettings;
	private RootTypeMapping mapping;
	private RootTypeMapping customMapping;

	public LowLevelIndexMetadataBuilder(JsonProvider jsonProvider, ElasticsearchIndexMetadataSyntax syntax,
			IndexNames indexNames) {
		this.jsonProvider = jsonProvider;
		this.syntax = syntax;
		this.indexNames = indexNames;
	}

	public void setAnalysisDefinitionRegistry(ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry) {
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
	}

	public void setCustomIndexSettings(IndexSettings customIndexSettings) {
		this.customIndexSettings = customIndexSettings;
	}

	public void setPropertyMappingIndexSettingsContributor(
			PropertyMappingIndexSettingsContributor propertyMappingIndexSettingsContributor) {
		this.propertyMappingIndexSettingsContributor = propertyMappingIndexSettingsContributor;
	}

	public void setMapping(RootTypeMapping mapping) {
		this.mapping = mapping;
	}

	public void setCustomMapping(RootTypeMapping customMapping) {
		this.customMapping = customMapping;
	}

	public IndexMetadata build() {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setAliases( buildAliases() );

		indexMetadata.setSettings( buildSettings() );

		indexMetadata.setMapping( buildMapping() );
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

		if ( propertyMappingIndexSettingsContributor != null ) {
			propertyMappingIndexSettingsContributor.contribute( settings );
		}

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

		if ( customIndexSettings == null ) {
			return settings;
		}

		// If customIndexSettings are present, merge them with the ones created by Search.
		// Avoid side effects: we copy the settings before modifying them.
		IndexSettings customIndexSettingsCopy =
				JsonUtils.deepCopy( jsonProvider.getCodecNoSerializeNulls(), IndexSettings.class, customIndexSettings );
		// The custom settings take precedence over the Hibernate Search ones.
		customIndexSettingsCopy.merge( settings );
		return customIndexSettingsCopy;
	}

	private RootTypeMapping buildMapping() {
		if ( customMapping == null ) {
			return mapping;
		}
		// Avoid side effects: we copy the mappings before modifying them.
		RootTypeMapping customMappingCopy =
				JsonUtils.deepCopy( jsonProvider.getCodecNoSerializeNulls(), RootTypeMapping.class, customMapping );
		RootTypeMapping hibernateSearchMappingCopy =
				JsonUtils.deepCopy( jsonProvider.getCodecNoSerializeNulls(), RootTypeMapping.class, mapping );
		// The custom mapping takes precedence over the Hibernate Search one.
		customMappingCopy.merge( hibernateSearchMappingCopy );
		return customMappingCopy;
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
