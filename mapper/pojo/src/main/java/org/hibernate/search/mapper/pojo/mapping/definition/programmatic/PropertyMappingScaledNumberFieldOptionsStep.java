/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a "scaled number" field.
 */
public interface PropertyMappingScaledNumberFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingScaledNumberFieldOptionsStep> {

	/**
	 * @param decimalScale How the scale of values should be adjusted before indexing as a fixed-precision integer.
	 * @return {@code this}, for method chaining.
	 * @see ScaledNumberField#decimalScale()
	 */
	PropertyMappingScaledNumberFieldOptionsStep decimalScale(int decimalScale);

}
