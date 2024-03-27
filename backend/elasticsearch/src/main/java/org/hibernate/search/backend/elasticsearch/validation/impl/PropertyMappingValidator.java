/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.JsonElement;

abstract class PropertyMappingValidator extends AbstractTypeMappingValidator<PropertyMapping> {

	private static final List<String> DEFAULT_DATE_FORMAT;
	static {
		List<String> formats = new ArrayList<>();
		formats.add( "strict_date_optional_time" );
		formats.add( "epoch_millis" );
		DEFAULT_DATE_FORMAT = CollectionHelper.toImmutableList( formats );
	}

	@Override
	protected Validator<PropertyMapping> getPropertyMappingValidator() {
		return this;
	}

	@Override
	public void validate(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
			PropertyMapping actualMapping) {
		LeafValidators.EQUAL.validateWithDefault(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "type",
				expectedMapping.getType(), actualMapping.getType(), DataTypes.OBJECT
		);

		List<String> formatDefault = DataTypes.DATE.equals( expectedMapping.getType() )
				? DEFAULT_DATE_FORMAT
				: Collections.emptyList();
		LeafValidators.FORMAT.validateWithDefault(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "format",
				expectedMapping.getFormat(), actualMapping.getFormat(), formatDefault
		);

		LeafValidators.EQUAL_DOUBLE.validate(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "scaling_factor",
				expectedMapping.getScalingFactor(), actualMapping.getScalingFactor()
		);

		validateIndexOptions( errorCollector, expectedMapping, actualMapping );

		LeafValidators.jsonElement( expectedMapping.getType() ).validate(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "null_value",
				expectedMapping.getNullValue(), actualMapping.getNullValue()
		);

		validateAnalyzerOptions( errorCollector, expectedMapping, actualMapping );

		LeafValidators.EQUAL.validateWithDefault(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "term_vector",
				expectedMapping.getTermVector(), actualMapping.getTermVector(), "no"
		);

		validateVectorMapping( errorCollector, expectedMapping, actualMapping );

		super.validate( errorCollector, expectedMapping, actualMapping );
	}

	private void validateAnalyzerOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
			PropertyMapping actualMapping) {
		LeafValidators.EQUAL.validateWithDefault(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "analyzer",
				expectedMapping.getAnalyzer(), actualMapping.getAnalyzer(), AnalyzerNames.DEFAULT
		);
		LeafValidators.EQUAL.validateWithDefault(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "search_analyzer",
				expectedMapping.getSearchAnalyzer(), actualMapping.getSearchAnalyzer(),
				expectedMapping.getAnalyzer() == null ? AnalyzerNames.DEFAULT : expectedMapping.getAnalyzer(),
				actualMapping.getAnalyzer() == null ? AnalyzerNames.DEFAULT : actualMapping.getAnalyzer()
		);
		LeafValidators.EQUAL.validate(
				errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "normalizer",
				expectedMapping.getNormalizer(), actualMapping.getNormalizer()
		);
	}

	private void validateIndexOptions(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
			PropertyMapping actualMapping) {
		Boolean expectedIndex = expectedMapping.getIndex();
		if ( Boolean.TRUE.equals( expectedIndex ) ) { // If we don't need an index, we don't care
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "index",
					expectedIndex, actualMapping.getIndex(), true
			);
		}

		Boolean expectedNorms = expectedMapping.getNorms();
		if ( Boolean.TRUE.equals( expectedNorms ) ) { // If we don't need norms, we don't care
			// From ES 5.0 on, norms are enabled by default on text fields only
			Boolean normsDefault = DataTypes.TEXT.equals( expectedMapping.getType() ) ? Boolean.TRUE : Boolean.FALSE;
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "norms",
					expectedNorms, actualMapping.getNorms(), normsDefault
			);
		}

		Boolean expectedDocValues = expectedMapping.getDocValues();
		if ( Boolean.TRUE.equals( expectedDocValues ) ) { // If we don't need doc_values, we don't care
			// From ES 5.0 on, all indexable doc_values is true by default
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "doc_values",
					expectedDocValues, actualMapping.getDocValues(), true
			);
		}
	}

	protected abstract void validateVectorMapping(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
			PropertyMapping actualMapping);

	static class Elasticsearch7PropertyMappingValidator extends PropertyMappingValidator {

		@Override
		protected void validateVectorMapping(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
				PropertyMapping actualMapping) {

		}
	}

	static class Elasticsearch812PropertyMappingValidator extends PropertyMappingValidator {

		private final ElasticsearchDenseVectorIndexOptionsValidator indexOptionsValidator =
				new ElasticsearchDenseVectorIndexOptionsValidator();

		@Override
		protected void validateVectorMapping(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
				PropertyMapping actualMapping) {
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "element_type",
					expectedMapping.getElementType(), actualMapping.getElementType(), "float"
			);

			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "dims",
					expectedMapping.getDims(), actualMapping.getDims()
			);

			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "similarity",
					expectedMapping.getSimilarity(), actualMapping.getSimilarity(), "cosine"
			);

			ElasticsearchDenseVectorIndexOptions indexOptions = expectedMapping.getIndexOptions();
			if ( indexOptions != null ) {
				indexOptionsValidator.validate( errorCollector, indexOptions, actualMapping.getIndexOptions() );
			}
		}
	}

	static class OpenSearch1PropertyMappingValidator extends PropertyMappingValidator {

		@Override
		protected void validateVectorMapping(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
				PropertyMapping actualMapping) {

		}
	}

	static class OpenSearch2PropertyMappingValidator extends PropertyMappingValidator {

		private final OpenSearchVectorTypeMethodValidator methodValidator = new OpenSearchVectorTypeMethodValidator();

		@Override
		protected void validateVectorMapping(ValidationErrorCollector errorCollector, PropertyMapping expectedMapping,
				PropertyMapping actualMapping) {
			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "dimension",
					expectedMapping.getDimension(), actualMapping.getDimension()
			);

			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "data_type",
					expectedMapping.getDataType(), actualMapping.getDataType()
			);

			OpenSearchVectorTypeMethod expectedMethod = expectedMapping.getMethod();
			if ( expectedMethod != null ) {
				methodValidator.validate( errorCollector, expectedMethod, actualMapping.getMethod() );
			}
		}
	}

	private static class ElasticsearchDenseVectorIndexOptionsValidator
			extends AbstractVectorAttributesValidator<ElasticsearchDenseVectorIndexOptions> {

		@Override
		protected String propertyName() {
			return "index_options";
		}

		@Override
		public void doValidate(ValidationErrorCollector errorCollector, ElasticsearchDenseVectorIndexOptions expected,
				ElasticsearchDenseVectorIndexOptions actual) {
			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "type",
					expected.getType(), actual.getType()
			);
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "m",
					expected.getM(), actual.getM(), 16
			);
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "ef_construction",
					expected.getEfConstruction(), actual.getEfConstruction(), 100
			);
		}

		@Override
		protected Map<String, JsonElement> expectedMappingExtraAttributes(ElasticsearchDenseVectorIndexOptions expected) {
			return expected.getExtraAttributes();
		}

		@Override
		protected Map<String, JsonElement> actualMappingExtraAttributes(ElasticsearchDenseVectorIndexOptions actual) {
			return actual.getExtraAttributes();
		}
	}

	private static class OpenSearchVectorTypeMethodValidator
			extends AbstractVectorAttributesValidator<OpenSearchVectorTypeMethod> {

		private final OpenSearchVectorTypeMethodParametersValidator parametersValidator =
				new OpenSearchVectorTypeMethodParametersValidator();

		@Override
		protected String propertyName() {
			return "method";
		}

		@Override
		public void doValidate(ValidationErrorCollector errorCollector, OpenSearchVectorTypeMethod expected,
				OpenSearchVectorTypeMethod actual) {
			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "name",
					expected.getName(), actual.getName()
			);

			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "space_type",
					expected.getSpaceType(), actual.getSpaceType(),
					"l2"
			);

			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "engine",
					expected.getEngine(), actual.getEngine()
			);

			OpenSearchVectorTypeMethod.Parameters expectedParameters = expected.getParameters();
			if ( expectedParameters != null ) {
				parametersValidator.validate( errorCollector, expectedParameters, actual.getParameters() );
			}
		}

		@Override
		protected Map<String, JsonElement> expectedMappingExtraAttributes(OpenSearchVectorTypeMethod expected) {
			return expected.getExtraAttributes();
		}

		@Override
		protected Map<String, JsonElement> actualMappingExtraAttributes(OpenSearchVectorTypeMethod actual) {
			return actual.getExtraAttributes();
		}
	}

	private static class OpenSearchVectorTypeMethodParametersValidator
			extends AbstractVectorAttributesValidator<OpenSearchVectorTypeMethod.Parameters> {
		@Override
		protected String propertyName() {
			return "parameters";
		}

		@Override
		public void doValidate(ValidationErrorCollector errorCollector, OpenSearchVectorTypeMethod.Parameters expected,
				OpenSearchVectorTypeMethod.Parameters actual) {
			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "m",
					expected.getM(), actual.getM()
			);

			LeafValidators.EQUAL.validate(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "ef_construction",
					expected.getEfConstruction(),
					actual.getEfConstruction()
			);
		}

		@Override
		protected Map<String, JsonElement> expectedMappingExtraAttributes(OpenSearchVectorTypeMethod.Parameters expected) {
			return expected.getExtraAttributes();
		}

		@Override
		protected Map<String, JsonElement> actualMappingExtraAttributes(OpenSearchVectorTypeMethod.Parameters actual) {
			return actual.getExtraAttributes();
		}
	}

	abstract static class AbstractVectorAttributesValidator<T> implements Validator<T> {
		private final Validator<JsonElement> extraAttributeValidator = new JsonElementValidator( new JsonElementEquivalence() );

		@Override
		public final void validate(ValidationErrorCollector errorCollector, T expected, T actual) {
			errorCollector.push( ValidationContextType.MAPPING_ATTRIBUTE, propertyName() );
			try {
				doValidate( errorCollector, expected, actual );

				extraAttributeValidator.validateAllIgnoreUnexpected(
						errorCollector, ValidationContextType.CUSTOM_INDEX_MAPPING_ATTRIBUTE,
						ElasticsearchValidationMessages.INSTANCE.customIndexMappingAttributeMissing(),
						expectedMappingExtraAttributes( expected ), actualMappingExtraAttributes( actual )
				);
			}
			finally {
				errorCollector.pop();
			}
		}

		protected abstract String propertyName();

		protected abstract void doValidate(ValidationErrorCollector errorCollector, T expected, T actual);

		protected abstract Map<String, JsonElement> expectedMappingExtraAttributes(T expected);

		protected abstract Map<String, JsonElement> actualMappingExtraAttributes(T actual);
	}
}
