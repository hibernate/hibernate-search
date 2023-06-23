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

class PropertyMappingValidator extends AbstractTypeMappingValidator<PropertyMapping> {

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
}
