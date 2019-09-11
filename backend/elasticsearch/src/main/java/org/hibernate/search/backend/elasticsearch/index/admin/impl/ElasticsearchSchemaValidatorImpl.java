/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.admin.gson.impl.AnalysisJsonElementEquivalence;
import org.hibernate.search.backend.elasticsearch.index.admin.gson.impl.AnalysisParameterEquivalenceRegistry;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchEventContexts;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.jboss.logging.Messages;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An {@link ElasticsearchSchemaValidator} implementation for Elasticsearch 5.6.
 * <p>
 * <strong>Important implementation note:</strong> unexpected attributes (i.e. those not mapped to a field in TypeMapping)
 * are totally ignored. This allows users to leverage Elasticsearch features that are not supported in
 * Hibernate Search, by setting those attributes manually.
 *
 */
public class ElasticsearchSchemaValidatorImpl implements ElasticsearchSchemaValidator {

	private static final ElasticsearchValidationMessages MESSAGES = Messages.getBundle( ElasticsearchValidationMessages.class );

	private static final AnalysisParameterEquivalenceRegistry NORMALIZER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder()
					.build();

	private final Validator<NormalizerDefinition> normalizerDefinitionValidator = new NormalizerDefinitionValidator( NORMALIZER_EQUIVALENCES );

	private static final double DEFAULT_DOUBLE_DELTA = 0.001;
	private static final float DEFAULT_FLOAT_DELTA = 0.001f;

	private static final List<String> DEFAULT_DATE_FORMAT;
	static {
		List<String> formats = new ArrayList<>();
		formats.add( "strict_date_optional_time" );
		formats.add( "epoch_millis" );
		DEFAULT_DATE_FORMAT = CollectionHelper.toImmutableList( formats );
	}

	private static final AnalysisParameterEquivalenceRegistry ANALYZER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder()
					.type( "keep_types" )
					.param( "types" ).unorderedArray()
					.end()
					.build();

	private static final AnalysisParameterEquivalenceRegistry CHAR_FILTER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder().build();

	private static final AnalysisParameterEquivalenceRegistry TOKENIZER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder()
					.type( "edgeNGram" )
					.param( "token_chars" ).unorderedArray()
					.end()
					.type( "nGram" )
					.param( "token_chars" ).unorderedArray()
					.end()
					.type( "stop" )
					.param( "stopwords" ).unorderedArray()
					.end()
					.type( "word_delimiter" )
					.param( "protected_words" ).unorderedArray()
					.end()
					.type( "keyword_marker" )
					.param( "keywords" ).unorderedArray()
					.end()
					.type( "pattern_capture" )
					.param( "patterns" ).unorderedArray()
					.end()
					.type( "common_grams" )
					.param( "common_words" ).unorderedArray()
					.end()
					.type( "cjk_bigram" )
					.param( "ignored_scripts" ).unorderedArray()
					.end()
					.build();

	private static final AnalysisParameterEquivalenceRegistry TOKEN_FILTER_EQUIVALENCES =
			new AnalysisParameterEquivalenceRegistry.Builder()
					.type( "keep_types" )
					.param( "types" ).unorderedArray()
					.end()
					.build();

	private final ElasticsearchSchemaAccessor schemaAccessor;

	private final Validator<RootTypeMapping> rootTypeMappingValidator = new RootTypeMappingValidator( new PropertyMappingValidator() );
	private final Validator<AnalyzerDefinition> analyzerDefinitionValidator = new AnalyzerDefinitionValidator( ANALYZER_EQUIVALENCES );
	private final Validator<CharFilterDefinition> charFilterDefinitionValidator = new AnalysisDefinitionValidator<>( CHAR_FILTER_EQUIVALENCES );
	private final Validator<TokenizerDefinition> tokenizerDefinitionValidator = new AnalysisDefinitionValidator<>( TOKENIZER_EQUIVALENCES );
	private final Validator<TokenFilterDefinition> tokenFilterDefinitionValidator = new AnalysisDefinitionValidator<>( TOKEN_FILTER_EQUIVALENCES );

	public ElasticsearchSchemaValidatorImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void validate(IndexMetadata expectedIndexMetadata, ContextualFailureCollector contextualFailureCollector) {
		URLEncodedString indexName = expectedIndexMetadata.getName();
		IndexMetadata actualIndexMetadata = schemaAccessor.getCurrentIndexMetadata( indexName );


		ValidationErrorCollector errorCollector = new ValidationErrorCollector(
				contextualFailureCollector.withContext( ElasticsearchEventContexts.getSchemaValidation() )
		);
		validate( errorCollector, expectedIndexMetadata, actualIndexMetadata );
	}

	@Override
	public boolean isSettingsValid(IndexMetadata expectedIndexMetadata) {
		URLEncodedString indexName = expectedIndexMetadata.getName();
		IndexMetadata actualIndexMetadata = schemaAccessor.getCurrentIndexMetadata( indexName );

		ValidationErrorCollector errorCollector = new ValidationErrorCollector();
		validateIndexSettings( errorCollector, expectedIndexMetadata.getSettings(), actualIndexMetadata.getSettings() );

		return !errorCollector.hasError();
	}

	private void validate(ValidationErrorCollector errorCollector, IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata) {
		validateIndexSettings( errorCollector, expectedIndexMetadata.getSettings(), actualIndexMetadata.getSettings() );

		RootTypeMapping expectedRootMapping = expectedIndexMetadata.getMapping();
		RootTypeMapping actualRootMapping = actualIndexMetadata.getMapping();
		if ( expectedRootMapping == null ) {
			return;
		}
		if ( actualRootMapping == null ) {
			errorCollector.addError( MESSAGES.mappingMissing() );
			return;
		}
		rootTypeMappingValidator.validate( errorCollector, expectedRootMapping, actualRootMapping );
	}

	private void validateIndexSettings(ValidationErrorCollector errorCollector, IndexSettings expectedSettings, IndexSettings actualSettings) {
		Analysis expectedAnalysis = expectedSettings.getAnalysis();
		if ( expectedAnalysis == null ) {
			// No expectation
			return;
		}
		Analysis actualAnalysis = actualSettings.getAnalysis();

		validateAnalysisSettings( errorCollector, expectedAnalysis, actualAnalysis );
	}

	private void validateAnalysisSettings(ValidationErrorCollector errorCollector, Analysis expectedAnalysis, Analysis actualAnalysis) {
		validateAll(
				errorCollector, ValidationContextType.ANALYZER, MESSAGES.analyzerMissing(), analyzerDefinitionValidator,
				expectedAnalysis.getAnalyzers(), actualAnalysis == null ? null : actualAnalysis.getAnalyzers() );

		validateAll(
				errorCollector, ValidationContextType.CHAR_FILTER, MESSAGES.charFilterMissing(), charFilterDefinitionValidator,
				expectedAnalysis.getCharFilters(), actualAnalysis == null ? null : actualAnalysis.getCharFilters() );

		validateAll(
				errorCollector, ValidationContextType.TOKENIZER, MESSAGES.tokenizerMissing(), tokenizerDefinitionValidator,
				expectedAnalysis.getTokenizers(), actualAnalysis == null ? null : actualAnalysis.getTokenizers() );

		validateAll(
				errorCollector, ValidationContextType.TOKEN_FILTER, MESSAGES.tokenFilterMissing(), tokenFilterDefinitionValidator,
				expectedAnalysis.getTokenFilters(), actualAnalysis == null ? null : actualAnalysis.getTokenFilters() );

		validateAll(
				errorCollector, ValidationContextType.NORMALIZER, MESSAGES.normalizerMissing(), normalizerDefinitionValidator,
				expectedAnalysis.getNormalizers(), actualAnalysis == null ? null : actualAnalysis.getNormalizers() );
	}

	private <T> void validateEqualWithoutDefault(ValidationErrorCollector errorCollector, String attributeName,
			T expectedValue, T actualValue) {
		validateEqualWithDefault( errorCollector, attributeName, expectedValue, actualValue, null );
	}

	/*
	 * Validate that two values are equal, using a given default value when null is encountered on either value.
	 * Useful to take into account the fact that Elasticsearch has default values for attributes.
	 */
	private <T> void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
			T expectedValue, T actualValue, T defaultValueForNulls) {
		Object defaultedExpectedValue = expectedValue == null ? defaultValueForNulls : expectedValue;
		Object defaultedActualValue = actualValue == null ? defaultValueForNulls : actualValue;
		if ( ! Objects.equals( defaultedExpectedValue, defaultedActualValue ) ) {
			// Don't show the defaulted actual value, this might confuse users
			errorCollector.addError( MESSAGES.invalidAttributeValue(
					attributeName, defaultedExpectedValue, actualValue
			) );
		}
	}

	/*
	 * Variation of validateEqualWithDefault() for floats.
	 */
	private void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
			Float expectedValue, Float actualValue, float delta, Float defaultValueForNulls) {
		Float defaultedExpectedValue = expectedValue == null ? defaultValueForNulls : expectedValue;
		Float defaultedActualValue = actualValue == null ? defaultValueForNulls : actualValue;
		if ( defaultedExpectedValue == null || defaultedActualValue == null ) {
			if ( defaultedExpectedValue == defaultedActualValue ) {
				// Both null
				return;
			}
			else {
				// One null and one non-null
				// Don't show the defaulted actual value, this might confuse users
				errorCollector.addError( MESSAGES.invalidAttributeValue(
						attributeName, defaultedExpectedValue, actualValue
				) );
			}
		}
		else {
			if ( Float.compare( defaultedExpectedValue, defaultedActualValue ) == 0 ) {
				return;
			}
			if ( Math.abs( defaultedExpectedValue - defaultedActualValue ) > delta ) {
				// Don't show the defaulted actual value, this might confuse users
				errorCollector.addError( MESSAGES.invalidAttributeValue(
						attributeName, defaultedExpectedValue, actualValue
				) );
			}
		}
	}

	/*
	 * Variation of validateEqualWithDefault() for doubles.
	 */
	private void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
			Double expectedValue, Double actualValue, double delta, Double defaultValueForNulls) {
		Double defaultedExpectedValue = expectedValue == null ? defaultValueForNulls : expectedValue;
		Double defaultedActualValue = actualValue == null ? defaultValueForNulls : actualValue;
		if ( defaultedExpectedValue == null || defaultedActualValue == null ) {
			if ( defaultedExpectedValue == defaultedActualValue ) {
				// Both null
				return;
			}
			else {
				// One null and one non-null
				// Don't show the defaulted actual value, this might confuse users
				errorCollector.addError( MESSAGES.invalidAttributeValue(
						attributeName, defaultedExpectedValue, actualValue
				) );
				return;
			}
		}
		if ( Double.compare( defaultedExpectedValue, defaultedActualValue ) == 0 ) {
			return;
		}
		if ( Math.abs( defaultedExpectedValue - defaultedActualValue ) > delta ) {
			// Don't show the defaulted actual value, this might confuse users
			errorCollector.addError( MESSAGES.invalidAttributeValue(
					attributeName, defaultedExpectedValue, actualValue
			) );
		}
	}

	/*
	 * Special validation for an Elasticsearch format:
	 * - Checks that the first element (the format used for output format in ES) is equal
	 * - Checks all expected formats are present in the actual value
	 */
	private void validateFormatWithDefault(ValidationErrorCollector errorCollector,
			String attributeName, List<String> expectedValue, List<String> actualValue, List<String> defaultValueForNulls) {
		List<String> defaultedExpectedValue = expectedValue == null ? defaultValueForNulls : expectedValue;
		List<String> defaultedActualValue = actualValue == null ? defaultValueForNulls : actualValue;
		if ( defaultedExpectedValue.isEmpty() ) {
			return;
		}

		String expectedOutputFormat = defaultedExpectedValue.get( 0 );
		String actualOutputFormat = defaultedActualValue.isEmpty() ? null : defaultedActualValue.get( 0 );
		if ( ! Objects.equals( expectedOutputFormat, actualOutputFormat ) ) {
			// Don't show the defaulted actual value, this might confuse users
			errorCollector.addError( MESSAGES.invalidOutputFormat(
					attributeName, expectedOutputFormat, actualOutputFormat
			) );
		}

		List<String> missingFormats = new ArrayList<>();
		missingFormats.addAll( defaultedExpectedValue );
		missingFormats.removeAll( defaultedActualValue );

		List<String> unexpectedFormats = new ArrayList<>();
		unexpectedFormats.addAll( defaultedActualValue );
		unexpectedFormats.removeAll( defaultedExpectedValue );

		if ( !missingFormats.isEmpty() || !unexpectedFormats.isEmpty() ) {
			errorCollector.addError( MESSAGES.invalidInputFormat(
					attributeName, defaultedExpectedValue, defaultedActualValue, missingFormats, unexpectedFormats
			) );
		}
	}

	private void validateJsonPrimitive(ValidationErrorCollector errorCollector,
			String type, String attributeName, JsonElement expectedValue, JsonElement actualValue) {
		String defaultedType = type == null ? DataTypes.OBJECT : type;
		doValidateJsonPrimitive( errorCollector, defaultedType, attributeName, expectedValue, actualValue );
	}

	private void doValidateJsonPrimitive(ValidationErrorCollector errorCollector,
			String type, String attributeName, JsonElement expectedValue, JsonElement actualValue) {
		// We can't just use equal, mainly because of floating-point numbers

		switch ( type ) {
			case DataTypes.TEXT:
			case DataTypes.KEYWORD:
				validateEqualWithoutDefault( errorCollector, attributeName, expectedValue, actualValue );
				break;
			case DataTypes.DOUBLE:
				if ( areNumbers( expectedValue, actualValue ) ) {
					validateEqualWithDefault( errorCollector, attributeName, expectedValue.getAsDouble(), actualValue.getAsDouble(),
							DEFAULT_DOUBLE_DELTA, null );
				}
				else {
					errorCollector.addError( MESSAGES.invalidAttributeValue(
							attributeName, expectedValue, actualValue
					) );
				}
				break;
			case DataTypes.FLOAT:
				if ( areNumbers( expectedValue, actualValue ) ) {
					validateEqualWithDefault( errorCollector, attributeName, expectedValue.getAsFloat(), actualValue.getAsFloat(),
							DEFAULT_FLOAT_DELTA, null );
				}
				else {
					errorCollector.addError( MESSAGES.invalidAttributeValue(
							attributeName, expectedValue, actualValue
					) );
				}
				break;
			case DataTypes.INTEGER:
			case DataTypes.LONG:
			case DataTypes.DATE:
			case DataTypes.BOOLEAN:
			case DataTypes.OBJECT:
			case DataTypes.GEO_POINT:
			default:
				validateEqualWithoutDefault( errorCollector, attributeName, expectedValue, actualValue );
				break;
		}
	}

	private boolean areNumbers(JsonElement expectedValue, JsonElement actualValue) {
		if ( !( expectedValue instanceof JsonPrimitive && actualValue instanceof JsonPrimitive ) ) {
			return false;
		}

		return ( (JsonPrimitive) expectedValue ).isNumber() && ( (JsonPrimitive) actualValue ).isNumber();
	}

	interface Validator<T> {
		void validate(ValidationErrorCollector errorCollector, T expected, T actual);
	}

	/*
	 * Validate all elements in a map.
	 *
	 * Unexpected elements are ignored, we only validate expected elements.
	 */
	private <T> void validateAll(
			ValidationErrorCollector errorCollector, ValidationContextType type, String messageIfMissing,
			Validator<T> validator,
			Map<String, T> expectedMap, Map<String, T> actualMap) {
		if ( expectedMap == null || expectedMap.isEmpty() ) {
			return;
		}
		if ( actualMap == null ) {
			actualMap = Collections.emptyMap();
		}
		for ( Map.Entry<String, T> entry : expectedMap.entrySet() ) {
			String name = entry.getKey();
			T expected = entry.getValue();
			T actual = actualMap.get( name );

			errorCollector.push( type, name );
			try {
				if ( actual == null ) {
					errorCollector.addError( messageIfMissing );
					continue;
				}

				validator.validate( errorCollector, expected, actual );
			}
			finally {
				errorCollector.pop();
			}
		}
	}

	class AnalysisDefinitionValidator<T extends AnalysisDefinition> implements Validator<T> {
		private final AnalysisParameterEquivalenceRegistry equivalences;

		AnalysisDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
			super();
			this.equivalences = equivalences;
		}

		@Override
		public void validate(ValidationErrorCollector errorCollector, T expectedDefinition, T actualDefinition) {
			String expectedType = expectedDefinition.getType();
			String actualType = actualDefinition.getType();
			Object defaultedExpectedType = expectedType == null ? getDefaultType() : expectedType;
			Object defaultedActualType = actualType == null ? getDefaultType() : actualType;
			if ( ! Objects.equals( defaultedExpectedType, defaultedActualType ) ) {
				errorCollector.addError( MESSAGES.invalidAnalysisDefinitionType(
						expectedType, actualType
				) );
			}

			Map<String, JsonElement> expectedParameters = expectedDefinition.getParameters();
			if ( expectedParameters == null ) {
				expectedParameters = Collections.emptyMap();
			}

			Map<String, JsonElement> actualParameters = actualDefinition.getParameters();
			if ( actualParameters == null ) {
				actualParameters = Collections.emptyMap();
			}

			// We also validate there isn't any unexpected parameters
			Set<String> parametersToValidate = new HashSet<>();
			parametersToValidate.addAll( expectedParameters.keySet() );
			parametersToValidate.addAll( actualParameters.keySet() );

			String typeName = expectedDefinition.getType();
			for ( String parameterName : parametersToValidate ) {
				JsonElement expected = expectedParameters.get( parameterName );
				JsonElement actual = actualParameters.get( parameterName );
				AnalysisJsonElementEquivalence parameterEquivalence = equivalences.get( typeName, parameterName );
				if ( ! parameterEquivalence.isEquivalent( expected, actual ) ) {
					errorCollector.addError( MESSAGES.invalidAnalysisDefinitionParameter( parameterName, expected, actual ) );
				}
			}
		}

		protected String getDefaultType() {
			return null;
		}
	}

	private class AnalyzerDefinitionValidator extends AnalysisDefinitionValidator<AnalyzerDefinition> {
		AnalyzerDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
			super( equivalences );
		}

		@Override
		public void validate(ValidationErrorCollector errorCollector, AnalyzerDefinition expectedDefinition, AnalyzerDefinition actualDefinition) {
			super.validate( errorCollector, expectedDefinition, actualDefinition );

			if ( ! Objects.equals( expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalyzerCharFilters(
						expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) );
			}

			if ( ! Objects.equals( expectedDefinition.getTokenizer(), actualDefinition.getTokenizer() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalyzerTokenizer(
						expectedDefinition.getTokenizer(), actualDefinition.getTokenizer() ) );
			}

			if ( ! Objects.equals( expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalyzerTokenFilters(
						expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) );
			}
		}

		@Override
		protected String getDefaultType() {
			return "custom";
		}
	}

	private abstract class AbstractTypeMappingValidator<T extends AbstractTypeMapping> implements Validator<T> {
		protected abstract Validator<PropertyMapping> getPropertyMappingValidator();

		@Override
		public void validate(ValidationErrorCollector errorCollector, T expectedMapping, T actualMapping) {
			DynamicType expectedDynamic = expectedMapping.getDynamic();
			if ( expectedDynamic != null ) { // If not provided, we don't care
				validateEqualWithDefault( errorCollector, "dynamic", expectedDynamic, actualMapping.getDynamic(), DynamicType.TRUE );
			}
			validateAll( errorCollector, ValidationContextType.MAPPING_PROPERTY, MESSAGES.propertyMissing(),
					getPropertyMappingValidator(),
					expectedMapping.getProperties(), actualMapping.getProperties() );
		}
	}

	private class RootTypeMappingValidator extends AbstractTypeMappingValidator<RootTypeMapping> {
		private final Validator<PropertyMapping> propertyMappingValidator;

		RootTypeMappingValidator(Validator<PropertyMapping> propertyMappingValidator) {
			super();
			this.propertyMappingValidator = propertyMappingValidator;
		}

		@Override
		protected Validator<PropertyMapping> getPropertyMappingValidator() {
			return propertyMappingValidator;
		}
	}

	private class PropertyMappingValidator extends AbstractTypeMappingValidator<PropertyMapping> {

		@Override
		protected Validator<PropertyMapping> getPropertyMappingValidator() {
			return this;
		}

		@Override
		public void validate(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
			validateEqualWithDefault( errorCollector, "type", expectedMapping.getType(), actualMapping.getType(), DataTypes.OBJECT );

			List<String> formatDefault = DataTypes.DATE.equals( expectedMapping.getType() )
					? DEFAULT_DATE_FORMAT : Collections.emptyList();
			validateFormatWithDefault( errorCollector, "format", expectedMapping.getFormat(), actualMapping.getFormat(), formatDefault );

			validateEqualWithoutDefault( errorCollector, "scaling_factor", expectedMapping.getScalingFactor(), actualMapping.getScalingFactor() );

			validateIndexOptions( errorCollector, expectedMapping, actualMapping );

			Boolean expectedStore = expectedMapping.getStore();
			if ( Boolean.TRUE.equals( expectedStore ) ) { // If we don't need storage, we don't care
				validateEqualWithDefault( errorCollector, "store", expectedStore, actualMapping.getStore(), false );
			}

			validateJsonPrimitive( errorCollector, expectedMapping.getType(), "null_value",
					expectedMapping.getNullValue(), actualMapping.getNullValue() );

			validateAnalyzerOptions( errorCollector, expectedMapping, actualMapping );

			validateEqualWithDefault( errorCollector, "term_vector", expectedMapping.getTermVector(), actualMapping.getTermVector(), "no" );

			super.validate( errorCollector, expectedMapping, actualMapping );

			// Validate fields with the same method as properties, since the content is about the same
			validateAll( errorCollector, ValidationContextType.MAPPING_PROPERTY_FIELD, MESSAGES.propertyFieldMissing(),
					getPropertyMappingValidator(),
					expectedMapping.getFields(), actualMapping.getFields() );
		}
	}

	private void validateIndexOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		Boolean expectedIndex = expectedMapping.getIndex();
		if ( Boolean.TRUE.equals( expectedIndex ) ) { // If we don't need an index, we don't care
			// From ES 5.0 on, all indexable fields are indexed by default
			validateEqualWithDefault( errorCollector, "index", expectedIndex, actualMapping.getIndex(), true );
		}

		Boolean expectedNorms = expectedMapping.getNorms();
		if ( Boolean.TRUE.equals( expectedNorms ) ) { // If we don't need norms, we don't care
			// From ES 5.0 on, norms are enabled by default on text fields only
			Boolean normsDefault = DataTypes.TEXT.equals( expectedMapping.getType() ) ? Boolean.TRUE : Boolean.FALSE;
			validateEqualWithDefault( errorCollector, "norms", expectedNorms, actualMapping.getNorms(), normsDefault );
		}

		Boolean expectedDocValues = expectedMapping.getDocValues();
		if ( Boolean.TRUE.equals( expectedDocValues ) ) { // If we don't need doc_values, we don't care
			// From ES 5.0 on, all indexable doc_values is true by default
			validateEqualWithDefault( errorCollector, "doc_values", expectedDocValues, actualMapping.getDocValues(), true );
		}
	}

	private void validateAnalyzerOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		validateEqualWithDefault( errorCollector, "analyzer", expectedMapping.getAnalyzer(), actualMapping.getAnalyzer(), "default" );
		validateEqualWithoutDefault( errorCollector, "normalizer", expectedMapping.getNormalizer(), actualMapping.getNormalizer() );
	}

	private class NormalizerDefinitionValidator extends AnalysisDefinitionValidator<NormalizerDefinition> {
		NormalizerDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
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

		@Override
		protected String getDefaultType() {
			return "custom";
		}
	}
}
