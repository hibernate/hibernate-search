package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a generic field.
 */
public interface PropertyMappingGenericFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingGenericFieldOptionsStep> {

	/**
	 * @param scorable Whether this field's numeric value can be used in score functions.
	 * @return {@code this}, for method chaining.
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField#scorable()
	 */
	@Incubating
	PropertyMappingGenericFieldOptionsStep scorable(Scorable scorable);

}
