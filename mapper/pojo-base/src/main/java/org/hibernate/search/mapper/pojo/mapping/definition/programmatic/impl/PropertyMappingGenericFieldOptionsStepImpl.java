/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingGenericFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<PropertyMappingGenericFieldOptionsStep>
		implements PropertyMappingGenericFieldOptionsStep {

	PropertyMappingGenericFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::getStandardTypeOptionsStep );
	}

	@Override
	PropertyMappingGenericFieldOptionsStep thisAsS() {
		return this;
	}

}
