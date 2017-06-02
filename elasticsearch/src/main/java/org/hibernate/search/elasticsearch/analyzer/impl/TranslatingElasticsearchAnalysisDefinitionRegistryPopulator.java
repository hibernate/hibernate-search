/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.analyzer.impl;

import java.util.Map;

import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.analyzer.definition.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.translation.ElasticsearchAnalyzerDefinitionTranslator;

/**
 * An {@link ElasticsearchAnalysisDefinitionRegistry} populator that translates {@literal @AnalyzerDef} annotations.
 * <p>
 * The responsibilities of this class are:
 * <ul>
 * <li>to break {@literal @AnalyzerDef}s into their components ({@literal @TokenizerDef}, etc.)
 * <li>to determine the correct name for each components (generating names automatically if needed)
 * <li>to store the translated definitions in the registry
 * </ul>
 * <p>
 * The actual translation job (e.g. {@literal @AnalyzerDef} to {@link AnalyzerDefinition})
 * is delegated to an {@link ElasticsearchAnalyzerDefinitionTranslator}.
 * <p>
 * The detection of duplicate definitions is handled in {@link ElasticsearchAnalysisDefinitionRegistry}.
 *
 * @author Yoann Rodiere
 */
public class TranslatingElasticsearchAnalysisDefinitionRegistryPopulator {

	private final ElasticsearchAnalysisDefinitionRegistry registry;

	private final ElasticsearchAnalyzerDefinitionTranslator translator;

	public TranslatingElasticsearchAnalysisDefinitionRegistryPopulator(ElasticsearchAnalysisDefinitionRegistry registry,
			ElasticsearchAnalyzerDefinitionTranslator translator) {
		this.registry = registry;
		this.translator = translator;
	}

	public void registerAnalyzerDef(AnalyzerDef hibernateSearchDefinition) {
		AnalyzerDefinition elasticsearchDefinition = new AnalyzerDefinition();

		String localName = hibernateSearchDefinition.name();
		String remoteName = localName; // We use the same definition name in Elasticsearch

		TokenizerDef hibernateSearchTokenizerDef = hibernateSearchDefinition.tokenizer();
		String tokenizerName = registerTokenizerDef( localName, hibernateSearchTokenizerDef );
		elasticsearchDefinition.setTokenizer( tokenizerName );

		for ( CharFilterDef hibernateSearchCharFilterDef : hibernateSearchDefinition.charFilters() ) {
			String charFilterName = registerCharFilterDef( localName, hibernateSearchCharFilterDef );
			elasticsearchDefinition.addCharFilter( charFilterName );
		}

		for ( TokenFilterDef hibernateSearchTokenFilterDef : hibernateSearchDefinition.filters() ) {
			String tokenFilterName = registerTokenFilterDef( localName, hibernateSearchTokenFilterDef );
			elasticsearchDefinition.addTokenFilter( tokenFilterName );
		}

		registry.register( remoteName, elasticsearchDefinition );
	}

	public void registerNormalizerDef(NormalizerDef hibernateSearchDefinition) {
		NormalizerDefinition elasticsearchDefinition = new NormalizerDefinition();

		String localName = hibernateSearchDefinition.name();
		String remoteName = localName; // We use the same definition name in Elasticsearch

		for ( CharFilterDef hibernateSearchCharFilterDef : hibernateSearchDefinition.charFilters() ) {
			String charFilterName = registerCharFilterDef( localName, hibernateSearchCharFilterDef );
			elasticsearchDefinition.addCharFilter( charFilterName );
		}

		for ( TokenFilterDef hibernateSearchTokenFilterDef : hibernateSearchDefinition.filters() ) {
			String tokenFilterName = registerTokenFilterDef( localName, hibernateSearchTokenFilterDef );
			elasticsearchDefinition.addTokenFilter( tokenFilterName );
		}

		registry.register( remoteName, elasticsearchDefinition );
	}

	private String registerTokenizerDef(String analyzerDefinitionName, TokenizerDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		TokenizerDefinition elasticsearchDefinition = translator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			registry.register( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private String registerCharFilterDef(String analyzerDefinitionName, CharFilterDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		CharFilterDefinition elasticsearchDefinition = translator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			registry.register( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private String registerTokenFilterDef(String analyzerDefinitionName, TokenFilterDef hibernateSearchDef) {
		String remoteName = hibernateSearchDef.name();

		TokenFilterDefinition elasticsearchDefinition = translator.translate( hibernateSearchDef );
		if ( remoteName.isEmpty() && !hasParameters( elasticsearchDefinition ) ) {
			// No parameters, and no specific name was provided => Use the builtin, default definition
			remoteName = elasticsearchDefinition.getType();
		}
		else {
			if ( remoteName.isEmpty() ) {
				remoteName = analyzerDefinitionName + "_" + hibernateSearchDef.factory().getSimpleName();
			}
			registry.register( remoteName, elasticsearchDefinition );
		}

		return remoteName;
	}

	private boolean hasParameters(AnalysisDefinition definition) {
		Map<?, ?> parameters = definition.getParameters();
		return parameters != null && !parameters.isEmpty();
	}

}
