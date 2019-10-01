/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.dsl.ScaledNumberIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PropertyMappingScaledNumberFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextFieldOptionsStep<PropertyMappingScaledNumberFieldOptionsStep, ScaledNumberIndexFieldTypeOptionsStep<?, ?>>
		implements PropertyMappingScaledNumberFieldOptionsStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyMappingScaledNumberFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, PropertyMappingScaledNumberFieldOptionsStepImpl::castIndexFieldTypeOptionsStep );
	}

	@Override
	PropertyMappingScaledNumberFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingScaledNumberFieldOptionsStep decimalScale(int decimalScale) {
		fieldModelContributor.add( (c, b) -> c.decimalScale( decimalScale ) );
		return thisAsS();
	}

	private static ScaledNumberIndexFieldTypeOptionsStep<?, ?> castIndexFieldTypeOptionsStep(
			StandardIndexFieldTypeOptionsStep<?,?> optionsStep) {
		if ( optionsStep instanceof ScaledNumberIndexFieldTypeOptionsStep ) {
			return (ScaledNumberIndexFieldTypeOptionsStep<?, ?>) optionsStep;
		}
		else {
			throw log.invalidFieldEncodingForScaledNumberFieldMapping(
					optionsStep, ScaledNumberIndexFieldTypeOptionsStep.class
			);
		}
	}
}
