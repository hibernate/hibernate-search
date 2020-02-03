/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;

public class IndexSettingsValidator implements Validator<IndexSettings> {

	final Validator<AnalyzerDefinition> analyzerDefinitionValidator = new AnalyzerDefinitionValidator();
	final Validator<NormalizerDefinition> normalizerDefinitionValidator = new NormalizerDefinitionValidator();
	final Validator<CharFilterDefinition> charFilterDefinitionValidator = AnalysisComponentDefinitionValidators.charFilterDefinitionValidator();
	final Validator<TokenizerDefinition> tokenizerDefinitionValidator = AnalysisComponentDefinitionValidators.tokenizerDefinitionValidator();
	final Validator<TokenFilterDefinition> tokenFilterDefinitionValidator = AnalysisComponentDefinitionValidators.tokenFilterDefinitionValidator();

	@Override
	public void validate(ValidationErrorCollector errorCollector, IndexSettings expected, IndexSettings actual) {
		Analysis expectedAnalysis = expected.getAnalysis();
		if ( expectedAnalysis == null ) {
			// No expectation
			return;
		}
		Analysis actualAnalysis = actual.getAnalysis();

		validateAnalysisSettings( errorCollector, expectedAnalysis, actualAnalysis );
	}

	private void validateAnalysisSettings(ValidationErrorCollector errorCollector,
			Analysis expectedAnalysis, Analysis actualAnalysis) {
		if ( expectedAnalysis == null || expectedAnalysis.isEmpty() ) {
			return;
		}

		analyzerDefinitionValidator.validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.ANALYZER,
				ElasticsearchValidationMessages.INSTANCE.analyzerMissing(),
				expectedAnalysis.getAnalyzers(), actualAnalysis == null ? null : actualAnalysis.getAnalyzers()
		);

		normalizerDefinitionValidator.validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.NORMALIZER,
				ElasticsearchValidationMessages.INSTANCE.normalizerMissing(),
				expectedAnalysis.getNormalizers(), actualAnalysis == null ? null : actualAnalysis.getNormalizers()
		);

		charFilterDefinitionValidator.validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.CHAR_FILTER,
				ElasticsearchValidationMessages.INSTANCE.charFilterMissing(),
				expectedAnalysis.getCharFilters(), actualAnalysis == null ? null : actualAnalysis.getCharFilters()
		);

		tokenizerDefinitionValidator.validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.TOKENIZER,
				ElasticsearchValidationMessages.INSTANCE.tokenizerMissing(),
				expectedAnalysis.getTokenizers(), actualAnalysis == null ? null : actualAnalysis.getTokenizers()
		);

		tokenFilterDefinitionValidator.validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.TOKEN_FILTER,
				ElasticsearchValidationMessages.INSTANCE.tokenFilterMissing(),
				expectedAnalysis.getTokenFilters(), actualAnalysis == null ? null : actualAnalysis.getTokenFilters()
		);
	}
}
