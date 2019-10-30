/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;


class PropertyMappingKeywordFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextFieldOptionsStep<PropertyMappingKeywordFieldOptionsStep>
		implements PropertyMappingKeywordFieldOptionsStep {

	PropertyMappingKeywordFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::getStringTypeOptionsStep );
	}

	@Override
	PropertyMappingKeywordFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep normalizer(String normalizerName) {
		fieldModelContributor.add( c -> c.getStringTypeOptionsStep().normalizer( normalizerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep norms(Norms norms) {
		fieldModelContributor.add( c -> c.getStringTypeOptionsStep().norms( norms ) );
		return thisAsS();
	}

}
