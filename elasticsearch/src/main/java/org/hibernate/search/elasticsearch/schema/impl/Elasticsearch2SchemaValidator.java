/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.json.AnalysisJsonElementEquivalence;
import org.hibernate.search.elasticsearch.schema.impl.json.AnalysisParameterEquivalenceRegistry;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalyzerDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings.Analysis;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.jboss.logging.Messages;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An {@link ElasticsearchSchemaValidator} implementation for Elasticsearch 2.
 * <p>
 * <strong>Important implementation note:</strong> unexpected attributes (i.e. those not mapped to a field in TypeMapping)
 * are totally ignored. This allows users to leverage Elasticsearch features that are not supported in
 * Hibernate Search, by setting those attributes manually.
 *
 * @author Yoann Rodiere
 */
public class Elasticsearch2SchemaValidator implements ElasticsearchSchemaValidator {

	private static final Log LOG = LoggerFactory.make( Log.class );

	static final ElasticsearchValidationMessages MESSAGES = Messages.getBundle( ElasticsearchValidationMessages.class );

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

	private final Validator<TypeMapping> typeMappingValidator = new TypeMappingValidator( new PropertyMappingValidator() );
	private final Validator<AnalyzerDefinition> analyzerDefinitionValidator = new AnalyzerDefinitionValidator( ANALYZER_EQUIVALENCES );
	private final Validator<CharFilterDefinition> charFilterDefinitionValidator = new AnalysisDefinitionValidator<>( CHAR_FILTER_EQUIVALENCES );
	private final Validator<TokenizerDefinition> tokenizerDefinitionValidator = new AnalysisDefinitionValidator<>( TOKENIZER_EQUIVALENCES );
	private final Validator<TokenFilterDefinition> tokenFilterDefinitionValidator = new AnalysisDefinitionValidator<>( TOKEN_FILTER_EQUIVALENCES );

	public Elasticsearch2SchemaValidator(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void validate(IndexMetadata expectedIndexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = expectedIndexMetadata.getName();
		IndexMetadata actualIndexMetadata = schemaAccessor.getCurrentIndexMetadata( indexName );

		ValidationErrorCollector errorCollector = new ValidationErrorCollector();
		errorCollector.push( ValidationContextType.INDEX, indexName.original );
		try {
			validate( errorCollector, expectedIndexMetadata, actualIndexMetadata );
		}
		finally {
			errorCollector.pop();
		}

		Map<ValidationContext, List<String>> messagesByContext = errorCollector.getMessagesByContext();
		if ( messagesByContext.isEmpty() ) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		for ( Map.Entry<ValidationContext, List<String>> entry : messagesByContext.entrySet() ) {
			ValidationContext context = entry.getKey();
			List<String> messages = entry.getValue();

			builder.append( "\n" ).append( formatContext( context ) );
			for ( String message : messages ) {
				builder.append( "\n\t" ).append( message );
			}
		}
		throw LOG.schemaValidationFailed( builder.toString() );
	}

	@Override
	public boolean isSettingsValid(IndexMetadata expectedIndexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = expectedIndexMetadata.getName();
		IndexMetadata actualIndexMetadata = schemaAccessor.getCurrentIndexMetadata( indexName );

		ValidationErrorCollector errorCollector = new ValidationErrorCollector();
		errorCollector.push( ValidationContextType.INDEX, indexName.original );
		try {
			validateIndexSettings( errorCollector, expectedIndexMetadata.getSettings(), actualIndexMetadata.getSettings() );
		}
		finally {
			errorCollector.pop();
		}

		return errorCollector.getMessagesByContext().isEmpty();
	}

	/**
	 * Format the validation context using the following format:
	 * {@code contextElement1, contextElement2, ... , contextElementN:}.
	 * <p>
	 * Each element is rendered using {@link #MESSAGES}.
	 * <p>
	 * Multiple consecutive property contexts are squeezed into a single path
	 * (e.g. "foo" followed by "bar" becomes "foo.bar".
	 *
	 * @param context The validation context to format.
	 * @return The validation context rendered as a string.
	 */
	private String formatContext(ValidationContext context) {
		StringBuilder builder = new StringBuilder();

		StringBuilder pathBuilder = new StringBuilder();
		for ( ValidationContextElement element : context.getElements() ) {
			String name = element.getName();

			if ( ValidationContextType.MAPPING_PROPERTY.equals( element.getType() ) ) {
				// Try to concatenate property names into a path before we actually append them
				if ( pathBuilder.length() > 0 ) {
					pathBuilder.append( "." );
				}
				pathBuilder.append( name );
			}
			else {
				if ( pathBuilder.length() > 0 ) {
					// Append the accumulated path
					appendContextElement( builder, ValidationContextType.MAPPING_PROPERTY, pathBuilder.toString() );
					pathBuilder.setLength( 0 ); // Clear
				}

				appendContextElement( builder, element.getType(), element.getName() );
			}
		}

		if ( pathBuilder.length() > 0 ) {
			// Append the remaining accumulated path
			appendContextElement( builder, ValidationContextType.MAPPING_PROPERTY, pathBuilder.toString() );
		}

		builder.append( ":" );

		return builder.toString();
	}

	private void appendContextElement(StringBuilder builder, ValidationContextType type, String name) {
		String formatted = formatContextElement( type, name );

		if ( builder.length() > 0 ) {
			builder.append( ", " );
		}
		builder.append( formatted );
	}

	protected String formatContextElement(ValidationContextType type, String name) {
		switch ( type ) {
			case INDEX:
				return MESSAGES.indexContext( name );
			case MAPPING:
				return MESSAGES.mappingContext( name );
			case MAPPING_PROPERTY:
				return MESSAGES.mappingPropertyContext( name );
			case MAPPING_PROPERTY_FIELD:
				return MESSAGES.mappingPropertyFieldContext( name );
			case ANALYZER:
				return MESSAGES.analyzerContext( name );
			case CHAR_FILTER:
				return MESSAGES.charFilterContext( name );
			case TOKENIZER:
				return MESSAGES.tokenizerContext( name );
			case TOKEN_FILTER:
				return MESSAGES.tokenFilterContext( name );
			default:
				throw new AssertionFailure( "Unexpected validation context element type: " + type );
		}
	}

	private void validate(ValidationErrorCollector errorCollector, IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata) {
		validateIndexSettings( errorCollector, expectedIndexMetadata.getSettings(), actualIndexMetadata.getSettings() );

		validateAll( errorCollector, ValidationContextType.MAPPING, MESSAGES.mappingMissing(), typeMappingValidator,
				expectedIndexMetadata.getMappings(), actualIndexMetadata.getMappings() );
	}

	private void validateIndexSettings(ValidationErrorCollector errorCollector, IndexSettings expectedSettings, IndexSettings actualSettings) {
		IndexSettings.Analysis expectedAnalysis = expectedSettings.getAnalysis();
		if ( expectedAnalysis == null ) {
			// No expectation
			return;
		}
		IndexSettings.Analysis actualAnalysis = actualSettings.getAnalysis();

		validateAnalysisSettings( errorCollector, expectedAnalysis, actualAnalysis );
	}

	protected void validateAnalysisSettings(ValidationErrorCollector errorCollector, Analysis expectedAnalysis, Analysis actualAnalysis) {
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
	}

	/*
	 * Validate that two values are equal, using a given default value when null is encountered on either value.
	 * Useful to take into account the fact that Elasticsearch has default values for attributes.
	 */
	protected <T> void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
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
	protected <T> void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
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
	protected <T> void validateEqualWithDefault(ValidationErrorCollector errorCollector, String attributeName,
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
	protected <T> void validateFormatWithDefault(ValidationErrorCollector errorCollector,
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

	protected final void validateJsonPrimitive(ValidationErrorCollector errorCollector,
			DataType type, String attributeName, JsonPrimitive expectedValue, JsonPrimitive actualValue) {
		DataType defaultedType = type == null ? DataType.OBJECT : type;
		doValidateJsonPrimitive( errorCollector, defaultedType, attributeName, expectedValue, actualValue );
	}

	@SuppressWarnings("deprecation")
	protected void doValidateJsonPrimitive(ValidationErrorCollector errorCollector,
			DataType type, String attributeName, JsonPrimitive expectedValue, JsonPrimitive actualValue) {
		// We can't just use equal, mainly because of floating-point numbers

		switch ( type ) {
			case DOUBLE:
				if ( expectedValue.isNumber() && actualValue.isNumber() ) {
					validateEqualWithDefault( errorCollector, attributeName, expectedValue.getAsDouble(), actualValue.getAsDouble(),
							DEFAULT_DOUBLE_DELTA, null );
				}
				else {
					errorCollector.addError( MESSAGES.invalidAttributeValue(
							attributeName, expectedValue, actualValue
							) );
				}
				break;
			case FLOAT:
				if ( expectedValue.isNumber() && actualValue.isNumber() ) {
					validateEqualWithDefault( errorCollector, attributeName, expectedValue.getAsFloat(), actualValue.getAsFloat(),
							DEFAULT_FLOAT_DELTA, null );
				}
				else {
					errorCollector.addError( MESSAGES.invalidAttributeValue(
							attributeName, expectedValue, actualValue
							) );
				}
				break;
			case INTEGER:
			case LONG:
			case DATE:
			case BOOLEAN:
			case STRING:
			case OBJECT:
			case GEO_POINT:
			default:
				validateEqualWithDefault( errorCollector, attributeName, expectedValue, actualValue, null );
				break;
		}
	}

	interface Validator<T> {
		void validate(ValidationErrorCollector errorCollector, T expected, T actual);
	}

	/*
	 * Validate all elements in a map.
	 *
	 * Unexpected elements are ignored, we only validate expected elements.
	 */
	protected <T> void validateAll(
			ValidationErrorCollector errorCollector, ValidationContextType type, String messageIfMissing,
			Validator<T> validator,
			Map<String, T> expectedMap, Map<String, T> actualMap) {
		if ( expectedMap == null || expectedMap.isEmpty() ) {
			return;
		}
		if ( actualMap == null ) {
			actualMap = Collections.<String, T>emptyMap();
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

		public AnalysisDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
			super();
			this.equivalences = equivalences;
		}

		@Override
		public void validate(ValidationErrorCollector errorCollector, T expectedDefinition, T actualDefinition) {
			if ( ! Objects.equals( expectedDefinition.getType(), actualDefinition.getType() ) ) {
				errorCollector.addError( MESSAGES.invalidAnalysisDefinitionType(
						expectedDefinition.getType(), actualDefinition.getType()
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
	}

	private class AnalyzerDefinitionValidator extends AnalysisDefinitionValidator<AnalyzerDefinition> {
		public AnalyzerDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
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
	}

	private abstract class AbstractTypeMappingValidator<T extends TypeMapping> implements Validator<T> {
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

	private class TypeMappingValidator extends AbstractTypeMappingValidator<TypeMapping> {
		private final Validator<PropertyMapping> propertyMappingValidator;

		public TypeMappingValidator(Validator<PropertyMapping> propertyMappingValidator) {
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
			validateEqualWithDefault( errorCollector, "type", expectedMapping.getType(), actualMapping.getType(), DataType.OBJECT );

			List<String> formatDefault = DataType.DATE.equals( expectedMapping.getType() )
					? DEFAULT_DATE_FORMAT : Collections.<String>emptyList();
			validateFormatWithDefault( errorCollector, "format", expectedMapping.getFormat(), actualMapping.getFormat(), formatDefault );

			validateEqualWithDefault( errorCollector, "boost", expectedMapping.getBoost(), actualMapping.getBoost(), DEFAULT_FLOAT_DELTA, 1.0f );

			validateIndexOptions( errorCollector, expectedMapping, actualMapping );

			Boolean expectedStore = expectedMapping.getStore();
			if ( Boolean.TRUE.equals( expectedStore ) ) { // If we don't need storage, we don't care
				validateEqualWithDefault( errorCollector, "store", expectedStore, actualMapping.getStore(), false );
			}

			validateJsonPrimitive( errorCollector, expectedMapping.getType(), "null_value",
					expectedMapping.getNullValue(), actualMapping.getNullValue() );

			validateAnalyzerOptions( errorCollector, expectedMapping, actualMapping );

			super.validate( errorCollector, expectedMapping, actualMapping );

			// Validate fields with the same method as properties, since the content is about the same
			validateAll( errorCollector, ValidationContextType.MAPPING_PROPERTY_FIELD, MESSAGES.propertyFieldMissing(),
					getPropertyMappingValidator(),
					expectedMapping.getFields(), actualMapping.getFields() );
		}
	}

	@SuppressWarnings("deprecation")
	protected void validateIndexOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		IndexType expectedIndex = expectedMapping.getIndex();
		if ( !IndexType.NO.equals( expectedIndex ) ) { // If we don't need an index, we don't care
			// See Elasticsearch doc: this attribute's default value depends on the data type.
			IndexType indexDefault = DataType.STRING.equals( expectedMapping.getType() ) ? IndexType.ANALYZED : IndexType.NOT_ANALYZED;
			validateEqualWithDefault( errorCollector, "index", expectedIndex, actualMapping.getIndex(), indexDefault );
		}


		Boolean expectedDocValues = expectedMapping.getDocValues();
		if ( Boolean.TRUE.equals( expectedDocValues ) ) { // If we don't need doc_values, we don't care
			/*
			 * Elasticsearch documentation (2.3) says doc_values is true by default on fields
			 * supporting it, but tests show it's wrong.
			 */
			validateEqualWithDefault( errorCollector, "doc_values", expectedDocValues, actualMapping.getDocValues(), false );
		}
	}

	protected void validateAnalyzerOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping, PropertyMapping actualMapping) {
		validateEqualWithDefault( errorCollector, "analyzer", expectedMapping.getAnalyzer(), actualMapping.getAnalyzer(), "default" );
	}

}
