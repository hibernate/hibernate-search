/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

/**
 * A component used by {@link Validator} implementations
 * to validate leaf values: attributes of a property mapping, parameters of an analyzer definition, ...
 *
 * @param <T> The type of values to validate.
 */
abstract class LeafValidator<T> {

	public final void validate(ValidationErrorCollector errorCollector,
			ValidationContextType type, String name,
			T expected, T actual) {
		validateWithDefault( errorCollector, type, name, expected, actual, null );
	}

	/*
	 * Validate that two values are equal, using a given default value when null is encountered on either value.
	 * Useful to take into account the fact that Elasticsearch has default values for attributes.
	 */
	public final void validateWithDefault(ValidationErrorCollector errorCollector,
			ValidationContextType type, String name,
			T expected, T actual, T defaultValueForNulls) {
		validateWithDefault( errorCollector, type, name, expected, actual, defaultValueForNulls, defaultValueForNulls );
	}

	/*
	 * Validate that two values are equal, using a given default value when null is encountered on either value.
	 * Useful to take into account the fact that Elasticsearch has default values for attributes.
	 */
	public final void validateWithDefault(ValidationErrorCollector errorCollector,
			ValidationContextType type, String name,
			T expected, T actual, T defaultValueForExpectedNull, T defaultValueForActualNull) {
		T defaultedExpected = expected == null ? defaultValueForExpectedNull : expected;
		T defaultedActual = actual == null ? defaultValueForActualNull : actual;
		if ( defaultedExpected == defaultedActual ) {
			// Covers null == null
			return;
		}
		errorCollector.push( type, name );
		try {
			doValidate(
					errorCollector, defaultedExpected, defaultedActual, actual
			);
		}
		finally {
			errorCollector.pop();
		}
	}

	protected abstract void doValidate(ValidationErrorCollector errorCollector,
			T defaultedExpected, T defaultedActual, Object actual);

}
