/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyGenericFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;


class PropertyGenericFieldMappingContextImpl
		extends AbstractPropertyNotFullTextFieldMappingContext<PropertyGenericFieldMappingContext, StandardIndexFieldTypeOptionsStep<?, ?>>
		implements PropertyGenericFieldMappingContext {

	PropertyGenericFieldMappingContextImpl(PropertyMappingContext parent, String relativeFieldName) {
		super(
				parent, relativeFieldName,
				PropertyGenericFieldMappingContextImpl::castIndexFieldTypeOptionsStep
		);
	}

	@Override
	PropertyGenericFieldMappingContext thisAsS() {
		return this;
	}

	private static StandardIndexFieldTypeOptionsStep<?,?> castIndexFieldTypeOptionsStep(
			StandardIndexFieldTypeOptionsStep<?,?> optionsStep) {
		// Nothing to do: we don't need anything more than the standard options
		return optionsStep;
	}

}
