/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;


class PropertyMappingGenericFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextFieldOptionsStep<PropertyMappingGenericFieldOptionsStep, StandardIndexFieldTypeOptionsStep<?, ?>>
		implements PropertyMappingGenericFieldOptionsStep {

	PropertyMappingGenericFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super(
				parent, relativeFieldName,
				PropertyMappingGenericFieldOptionsStepImpl::castIndexFieldTypeOptionsStep
		);
	}

	@Override
	PropertyMappingGenericFieldOptionsStep thisAsS() {
		return this;
	}

	private static StandardIndexFieldTypeOptionsStep<?,?> castIndexFieldTypeOptionsStep(
			StandardIndexFieldTypeOptionsStep<?,?> optionsStep) {
		// Nothing to do: we don't need anything more than the standard options
		return optionsStep;
	}

}
