/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzer;
import org.hibernate.search.elasticsearch.analyzer.impl.ElasticsearchAnalyzerReference;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexSettingsBuilder {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ElasticsearchAnalyzerDefinitionTranslator analyzerDefinitionTranslator;

	private final Map<String, AnalyzerDefinition> analyzers = new TreeMap<>();

	private final Map<String, TokenizerDefinition> tokenizers = new TreeMap<>();

	private final Map<String, TokenFilterDefinition> tokenFilters = new TreeMap<>();

	private final Map<String, CharFilterDefinition> charFilters = new TreeMap<>();

	private EntityIndexBinding binding;

	public ElasticsearchIndexSettingsBuilder(ElasticsearchAnalyzerDefinitionTranslator analyzerDefinitionTranslator) {
		super();
		this.analyzerDefinitionTranslator = analyzerDefinitionTranslator;
	}

	public Class<?> getBeanClass() {
		return binding.getDocumentBuilder().getBeanClass();
	}

	public void setBinding(EntityIndexBinding binding) {
		this.binding = binding;
	}

	public IndexSettings build() {
		IndexSettings settings = new IndexSettings();

		if ( !analyzers.isEmpty() ) {
			getAnalysis( settings ).setAnalyzers( analyzers );
		}
		if ( !tokenizers.isEmpty() ) {
			getAnalysis( settings ).setTokenizers( tokenizers );
		}
		if ( !tokenFilters.isEmpty() ) {
			getAnalysis( settings ).setTokenFilters( tokenFilters );
		}
		if ( !charFilters.isEmpty() ) {
			getAnalysis( settings ).setCharFilters( charFilters );
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

	public String registerAnalyzer(AnalyzerReference analyzerReference, String fieldName) {
		if ( !analyzerReference.is( ElasticsearchAnalyzerReference.class ) ) {
			LOG.analyzerIsNotElasticsearch( getBeanClass(), fieldName, analyzerReference );
			return null;
		}
		ElasticsearchAnalyzerReference elasticsearchReference = analyzerReference.unwrap( ElasticsearchAnalyzerReference.class );
		ElasticsearchAnalyzer analyzer = elasticsearchReference.getAnalyzer();

		AnalyzerDef hibernateSearchDefinition = analyzer.getDefinition( fieldName );
		if ( hibernateSearchDefinition == null ) {
			// Assuming a reference to a builtin or pre-defined analyzer
			return analyzer.getName( fieldName );
		}
		else {
			return addAnalyzerDef( hibernateSearchDefinition );
		}
	}

	private String addAnalyzerDef(AnalyzerDef hibernateSearchDefinition) {
		AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();

		String localName = hibernateSearchDefinition.name();
		String remoteName = localName; // We use the same definition name in Elasticsearch
		if ( analyzers.containsKey( remoteName ) ) {
			/*
			 * We already check for naming conflicts in the engine,
			 * so we are sure that the pre-existing analyzer definition
			 * is the same.
			 */
			return remoteName;
		}

		TokenizerDef hibernateSearchTokenizerDef = hibernateSearchDefinition.tokenizer();
		String tokenizerName = addTokenizerDef( localName, hibernateSearchTokenizerDef );
		analyzerDefinition.setTokenizer( tokenizerName );

		for ( CharFilterDef hibernateSearchCharFilterDef : hibernateSearchDefinition.charFilters() ) {
			String charFilterName = addCharFilterDef( localName, hibernateSearchCharFilterDef );
			analyzerDefinition.addCharFilter( charFilterName );
		}

		for ( TokenFilterDef hibernateSearchTokenFilterDef : hibernateSearchDefinition.filters() ) {
			String tokenFilterName = addTokenFilterDef( localName, hibernateSearchTokenFilterDef );
			analyzerDefinition.addTokenFilter( tokenFilterName );
		}

		analyzers.put( remoteName, analyzerDefinition );

		return remoteName;
	}

	private String addTokenizerDef(String analyzerDefinitionName, TokenizerDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		TokenizerDefinition elasticsearchDefinition = analyzerDefinitionTranslator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			if ( tokenizers.containsKey( remoteName ) ) {
				throw LOG.tokenizerNamingConflict( remoteName );
			}
			tokenizers.put( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private String addCharFilterDef(String analyzerDefinitionName, CharFilterDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		CharFilterDefinition elasticsearchDefinition = analyzerDefinitionTranslator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			if ( charFilters.containsKey( remoteName ) ) {
				throw LOG.charFilterNamingConflict( remoteName );
			}
			charFilters.put( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private String addTokenFilterDef(String analyzerDefinitionName, TokenFilterDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		TokenFilterDefinition elasticsearchDefinition = analyzerDefinitionTranslator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			if ( tokenFilters.containsKey( remoteName ) ) {
				throw LOG.tokenFilterNamingConflict( remoteName );
			}
			tokenFilters.put( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private boolean hasParameters(AnalysisDefinition definition) {
		Map<?, ?> parameters = definition.getParameters();
		return parameters != null && !parameters.isEmpty();
	}

}
