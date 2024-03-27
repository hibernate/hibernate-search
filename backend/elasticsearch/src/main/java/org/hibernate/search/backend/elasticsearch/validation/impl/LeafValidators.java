/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class LeafValidators {

	private LeafValidators() {
	}

	private static final double DEFAULT_DOUBLE_DELTA = 0.001;
	private static final float DEFAULT_FLOAT_DELTA = 0.001f;

	public static final LeafValidator<Object> EQUAL = new LeafValidator<Object>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, Object defaultedExpected,
				Object defaultedActual, Object actual) {
			if ( !Objects.equals( defaultedExpected, defaultedActual ) ) {
				// Don't display the default for the actual value: it could confuse users.
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidValue(
						defaultedExpected, actual
				) );
			}
		}
	};

	public static final LeafValidator<Double> EQUAL_DOUBLE = new LeafValidator<Double>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, Double defaultedExpected,
				Double defaultedActual, Object actual) {
			if ( defaultedExpected == null
					|| defaultedActual == null // One null and one non-null
					|| Double.compare( defaultedExpected, defaultedActual ) != 0
							&& Math.abs( defaultedExpected - defaultedActual ) > DEFAULT_DOUBLE_DELTA ) {
				// Don't display the default for the actual value: it could confuse users.
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidValue(
						defaultedExpected, actual
				) );
			}
		}
	};

	public static final LeafValidator<Float> EQUAL_FLOAT = new LeafValidator<Float>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, Float defaultedExpected,
				Float defaultedActual, Object actual) {
			if ( defaultedExpected == null
					|| defaultedActual == null // One null and one non-null
					|| Float.compare( defaultedExpected, defaultedActual ) != 0
							&& Math.abs( defaultedExpected - defaultedActual ) > DEFAULT_FLOAT_DELTA ) {
				// Don't display the default for the actual value: it could confuse users.
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidValue(
						defaultedExpected, actual
				) );
			}
		}
	};

	/*
	 * Special validation for an Elasticsearch format:
	 * - Checks that the first element (the format used for output format in ES) is equal
	 * - Checks all expected formats are present in the actual value
	 */
	public static final LeafValidator<List<String>> FORMAT = new LeafValidator<List<String>>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, List<String> defaultedExpected,
				List<String> defaultedActual, Object actual) {
			if ( defaultedExpected.isEmpty() ) {
				return;
			}

			String expectedOutputFormat = defaultedExpected.get( 0 );
			String actualOutputFormat = defaultedActual.isEmpty() ? null : defaultedActual.get( 0 );
			if ( !Objects.equals( expectedOutputFormat, actualOutputFormat ) ) {
				// Don't show the defaulted actual value, this might confuse users
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidOutputFormat(
						expectedOutputFormat, actualOutputFormat
				) );
			}

			List<String> missingFormats = new ArrayList<>( defaultedExpected );
			missingFormats.removeAll( defaultedActual );

			List<String> unexpectedFormats = new ArrayList<>( defaultedActual );
			unexpectedFormats.removeAll( defaultedExpected );

			if ( !missingFormats.isEmpty() || !unexpectedFormats.isEmpty() ) {
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidFormat(
						defaultedExpected, defaultedActual, missingFormats, unexpectedFormats
				) );
			}
		}
	};

	public static LeafValidator<? super JsonElement> jsonElement(String type) {
		String defaultedType = type == null ? DataTypes.OBJECT : type;
		switch ( defaultedType ) {
			case DataTypes.DOUBLE:
				return JSON_ELEMENT_AS_DOUBLE_LEAF_VALIDATOR;
			case DataTypes.FLOAT:
				return JSON_ELEMENT_AS_FLOAT_LEAF_VALIDATOR;
			case DataTypes.TEXT:
			case DataTypes.KEYWORD:
			case DataTypes.INTEGER:
			case DataTypes.LONG:
			case DataTypes.DATE:
			case DataTypes.BOOLEAN:
			case DataTypes.OBJECT:
			case DataTypes.GEO_POINT:
			default:
				return EQUAL;
		}
	}

	private static final LeafValidator<JsonElement> JSON_ELEMENT_AS_DOUBLE_LEAF_VALIDATOR = new LeafValidator<JsonElement>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, JsonElement defaultedExpected,
				JsonElement defaultedActual, Object actual) {
			if ( areNumbers( defaultedExpected, defaultedActual ) ) {
				EQUAL_DOUBLE.doValidate(
						errorCollector, defaultedExpected.getAsDouble(), defaultedActual.getAsDouble(), actual
				);
			}
			else {
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidValue(
						defaultedExpected, actual
				) );
			}
		}
	};

	private static final LeafValidator<JsonElement> JSON_ELEMENT_AS_FLOAT_LEAF_VALIDATOR = new LeafValidator<JsonElement>() {
		@Override
		protected void doValidate(ValidationErrorCollector errorCollector, JsonElement defaultedExpected,
				JsonElement defaultedActual, Object actual) {
			if ( areNumbers( defaultedExpected, defaultedActual ) ) {
				EQUAL_FLOAT.doValidate(
						errorCollector, defaultedExpected.getAsFloat(), defaultedActual.getAsFloat(), actual
				);
			}
			else {
				errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidValue(
						defaultedExpected, actual
				) );
			}
		}
	};

	private static boolean areNumbers(JsonElement expectedValue, JsonElement actualValue) {
		if ( !( expectedValue instanceof JsonPrimitive && actualValue instanceof JsonPrimitive ) ) {
			return false;
		}

		return ( (JsonPrimitive) expectedValue ).isNumber() && ( (JsonPrimitive) actualValue ).isNumber();
	}

}
