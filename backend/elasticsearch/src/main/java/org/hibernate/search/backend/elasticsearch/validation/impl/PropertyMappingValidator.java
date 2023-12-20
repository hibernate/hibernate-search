/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

	static class Elasticsearch8PropertyMappingValidator extends PropertyMappingValidator {

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

			JsonElement expectedIndexOptionsElement = expectedMapping.getIndexOptions();
			if ( expectedIndexOptionsElement != null && expectedIndexOptionsElement.isJsonObject() ) {
				JsonObject expectedIndexOptions = expectedIndexOptionsElement.getAsJsonObject();
				JsonObject actualIndexOptions = actualMapping.getIndexOptions().getAsJsonObject();
				LeafValidators.EQUAL.validate(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "index_options.type",
						expectedIndexOptions.get( "type" ), actualIndexOptions.get( "type" )
				);
				LeafValidators.EQUAL.validateWithDefault(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "index_options.m",
						expectedIndexOptions.get( "m" ), actualIndexOptions.get( "m" ), 16
				);
				LeafValidators.EQUAL.validateWithDefault(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "index_options.ef_construction",
						expectedIndexOptions.get( "ef_construction" ), actualIndexOptions.get( "ef_construction" ), 100
				);
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

			JsonElement methodElement = expectedMapping.getMethod();
			if ( methodElement != null && methodElement.isJsonObject() ) {
				JsonObject expectedMethod = methodElement.getAsJsonObject();
				JsonObject actualMethod = actualMapping.getMethod().getAsJsonObject();

				LeafValidators.EQUAL.validate(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "method.name",
						getAsString( expectedMethod, "name" ), getAsString( actualMethod, "name" )
				);

				LeafValidators.EQUAL.validateWithDefault(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "method.space_type",
						getAsString( expectedMethod, "space_type" ), getAsString( actualMethod, "space_type" ),
						"l2"
				);

				LeafValidators.EQUAL.validate(
						errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "method.engine",
						getAsString( expectedMethod, "engine" ), getAsString( actualMethod, "engine" )
				);

				JsonElement parametersElement = expectedMethod.get( "parameters" );
				if ( parametersElement != null && parametersElement.isJsonObject() ) {
					JsonObject expectedParameters = parametersElement.getAsJsonObject();
					JsonObject actualParameters = actualMethod.get( "parameters" ).getAsJsonObject();

					LeafValidators.EQUAL.validate(
							errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "method.parameters.m",
							getAsInteger( expectedParameters, "m" ), getAsInteger( actualParameters, "m" )
					);

					LeafValidators.EQUAL.validate(
							errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "method.parameters.ef_construction",
							getAsInteger( expectedParameters, "ef_construction" ),
							getAsInteger( actualParameters, "ef_construction" )
					);
				}
			}
		}

		private static String getAsString(JsonObject object, String property) {
			JsonElement element = object.get( property );
			return element == null || element.isJsonNull() ? null : element.getAsString();
		}

		private static Integer getAsInteger(JsonObject object, String property) {
			JsonElement element = object.get( property );
			return element == null || element.isJsonNull() ? null : element.getAsInt();
		}
	}
}
