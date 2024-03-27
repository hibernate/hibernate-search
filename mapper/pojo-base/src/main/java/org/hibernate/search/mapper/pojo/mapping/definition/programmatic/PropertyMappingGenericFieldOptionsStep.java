/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a generic field.
 */
public interface PropertyMappingGenericFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingGenericFieldOptionsStep> {

}
