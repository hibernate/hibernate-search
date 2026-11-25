package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a "scaled number" field.
 */
public interface PropertyMappingScaledNumberFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingScaledNumberFieldOptionsStep> {

	/**
	 * @param scorable Whether this field's numeric value can be used in score functions.
	 * @return {@code this}, for method chaining.
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField#scorable()
	 */
	@Incubating
	PropertyMappingScaledNumberFieldOptionsStep scorable(Scorable scorable);

	/**
	 * @param decimalScale How the scale of values should be adjusted before indexing as a fixed-precision integer.
	 * @return {@code this}, for method chaining.
	 * @see ScaledNumberField#decimalScale()
	 */
	PropertyMappingScaledNumberFieldOptionsStep decimalScale(int decimalScale);

}
