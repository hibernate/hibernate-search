/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Objects;

import org.hibernate.search.elasticsearch.schema.impl.json.AnalysisParameterEquivalenceRegistry;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings.Analysis;
import org.hibernate.search.elasticsearch.settings.impl.model.NormalizerDefinition;

/**
 * An {@link ElasticsearchSchemaValidator} implementation for Elasticsearch 5.2.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch52SchemaValidator extends Elasticsearch50SchemaValidator {

	private static final AnalysisParameterEquivalenceRegistry NORMALIZER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder()
					.build();

	private final Validator<NormalizerDefinition> normalizerDefinitionValidator = new NormalizerDefinitionValidator( NORMALIZER_EQUIVALENCES );

	public Elasticsearch52SchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		super( schemaAccessor );
	}

	@Override
	protected String formatContextElement(ValidationContextType type, String name) {
		if ( ValidationContextType.NORMALIZER.equals( type ) ) {
			return MESSAGES.normalizerContext( name );
		}
		return super.formatContextElement( type, name );
	}

	@Override
	protected void validateAnalysisSettings(ValidationErrorCollector errorCollector, Analysis expectedAnalysis, Analysis actualAnalysis) {
		super.validateAnalysisSettings( errorCollector, expectedAnalysis, actualAnalysis );

		validateAll(
				errorCollector, ValidationContextType.NORMALIZER, MESSAGES.normalizerMissing(), normalizerDefinitionValidator,
				expectedAnalysis.getNormalizers(), actualAnalysis == null ? null : actualAnalysis.getNormalizers() );
	}

	@Override
	protected void validateAnalyzerOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		super.validateAnalyzerOptions( errorCollector, expectedMapping, actualMapping );

		validateEqualWithDefault( errorCollector, "normalizer", expectedMapping.getNormalizer(), actualMapping.getNormalizer(), null );
	}

	private class NormalizerDefinitionValidator extends AnalysisDefinitionValidator<NormalizerDefinition> {
		public NormalizerDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
			super( equivalences );
		}

		@Override
		public void validate(ValidationErrorCollector errorCollector, NormalizerDefinition expectedDefinition, NormalizerDefinition actualDefinition) {
			super.validate( errorCollector, expectedDefinition, actualDefinition );

			if ( ! Objects.equals( expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalyzerCharFilters(
						expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) );
			}

			if ( ! Objects.equals( expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalyzerTokenFilters(
						expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) );
			}
		}
	}
}
