/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingNonStandardFieldOptionsStep
		extends AbstractPropertyMappingFieldOptionsStep<PropertyMappingNonStandardFieldOptionsStep> {

	PropertyMappingNonStandardFieldOptionsStep(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName,
				// We'll use the "standard" as(), because it's simpler.
				// It will always fail the type check, so it's kind of nonsensical,
				// but I (Yoann) can't be bothered to introduce a specific exception
				// just for this.
				IndexFieldTypeFactory::as,
				FieldModelContributorContext::checkNonStandardTypeOptionsStep );
	}

	@Override
	PropertyMappingNonStandardFieldOptionsStep thisAsS() {
		return this;
	}

}
