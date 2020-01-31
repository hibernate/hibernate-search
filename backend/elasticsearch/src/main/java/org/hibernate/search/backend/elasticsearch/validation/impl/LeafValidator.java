/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		T defaultedExpected = expected == null ? defaultValueForNulls : expected;
		T defaultedActual = actual == null ? defaultValueForNulls : actual;
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
