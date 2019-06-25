/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class PropertyMappingKeywordFieldOptionsStepImpl
		extends AbstractPropertyMappingNonFullTextFieldOptionsStep<PropertyMappingKeywordFieldOptionsStep, StringIndexFieldTypeOptionsStep<?>>
		implements PropertyMappingKeywordFieldOptionsStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyMappingKeywordFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, PropertyMappingKeywordFieldOptionsStepImpl::castIndexFieldTypeOptionsStep );
	}

	@Override
	PropertyMappingKeywordFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep normalizer(String normalizerName) {
		fieldModelContributor.add( (c, b) -> c.normalizer( normalizerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingKeywordFieldOptionsStep norms(Norms norms) {
		fieldModelContributor.add( (c, b) -> c.norms( norms ) );
		return thisAsS();
	}

	private static StringIndexFieldTypeOptionsStep<?> castIndexFieldTypeOptionsStep(
			StandardIndexFieldTypeOptionsStep<?,?> optionsStep) {
		if ( optionsStep instanceof StringIndexFieldTypeOptionsStep ) {
			return (StringIndexFieldTypeOptionsStep<?>) optionsStep;
		}
		else {
			throw log.invalidFieldEncodingForKeywordFieldMapping(
					optionsStep, StringIndexFieldTypeOptionsStep.class
			);
		}
	}

}
