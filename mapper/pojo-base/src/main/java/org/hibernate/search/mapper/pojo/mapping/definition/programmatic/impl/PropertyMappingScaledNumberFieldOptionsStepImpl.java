package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import org.hibernate.search.engine.backend.types.Scorable;

import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class PropertyMappingScaledNumberFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextStandardFieldOptionsStep<PropertyMappingScaledNumberFieldOptionsStep>
		implements PropertyMappingScaledNumberFieldOptionsStep {

	PropertyMappingScaledNumberFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::scaledNumberTypeOptionsStep );
	}

	@Override
	PropertyMappingScaledNumberFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep scorable(Scorable scorable) {
		fieldModelContributor.add( c -> c.standardTypeOptionsStep().scorable( scorable ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep decimalScale(int decimalScale) {
		fieldModelContributor.add( c -> c.scaledNumberTypeOptionsStep().decimalScale( decimalScale ) );
		return thisAsS();
	}
}
