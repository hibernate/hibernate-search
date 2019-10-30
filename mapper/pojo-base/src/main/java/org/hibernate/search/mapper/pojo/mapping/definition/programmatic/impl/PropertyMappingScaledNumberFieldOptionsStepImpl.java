/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingScaledNumberFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<PropertyMappingScaledNumberFieldOptionsStep>
		implements PropertyMappingScaledNumberFieldOptionsStep {

	PropertyMappingScaledNumberFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::getScaledNumberTypeOptionsStep );
	}

	@Override
	PropertyMappingScaledNumberFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep decimalScale(int decimalScale) {
		fieldModelContributor.add( c -> c.getScaledNumberTypeOptionsStep().decimalScale( decimalScale ) );
		return thisAsS();
	}
}
