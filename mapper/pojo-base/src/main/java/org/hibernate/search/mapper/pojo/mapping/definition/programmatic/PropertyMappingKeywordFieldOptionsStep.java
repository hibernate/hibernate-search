/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * The step in a property-to-index-field mapping where optional parameters can be set,
 * when the index field is a keyword field.
 */
public interface PropertyMappingKeywordFieldOptionsStep
		extends PropertyMappingNonFullTextFieldOptionsStep<PropertyMappingKeywordFieldOptionsStep> {

	/**
	 * @param normalizerName A reference to the normalizer to use for this field.
	 * @return {@code this}, for method chaining.
	 * @see KeywordField#normalizer()
	 */
	PropertyMappingKeywordFieldOptionsStep normalizer(String normalizerName);

	/**
	 * @param norms Whether index time scoring information should be stored or not.
	 * @return {@code this}, for method chaining.
	 * @see KeywordField#norms()
	 * @see Norms
	 */
	PropertyMappingKeywordFieldOptionsStep norms(Norms norms);

}
