package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Scorable;

import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingGenericFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingGenericFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<PropertyMappingGenericFieldOptionsStep>
		implements PropertyMappingGenericFieldOptionsStep {

	PropertyMappingGenericFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::standardTypeOptionsStep );
	}

	@Override
	PropertyMappingGenericFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingGenericFieldOptionsStep scorable(Scorable scorable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().scorable( scorable ) );
		return thisAsS();
	}

}
