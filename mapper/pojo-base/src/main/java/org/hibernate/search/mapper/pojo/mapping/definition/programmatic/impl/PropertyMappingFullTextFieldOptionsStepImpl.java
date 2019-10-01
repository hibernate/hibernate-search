/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class PropertyMappingFullTextFieldOptionsStepImpl
		extends AbstractPropertyMappingFieldOptionsStep<PropertyMappingFullTextFieldOptionsStep, StringIndexFieldTypeOptionsStep<?>>
		implements PropertyMappingFullTextFieldOptionsStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	PropertyMappingFullTextFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super(
				parent, relativeFieldName,
				PropertyMappingFullTextFieldOptionsStepImpl::castIndexFieldTypeOptionsStep
		);
	}

	@Override
	PropertyMappingFullTextFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep analyzer(String normalizerName) {
		fieldModelContributor.add( (c, b) -> c.analyzer( normalizerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep searchAnalyzer(String searchAnalyzerName) {
		fieldModelContributor.add( (c, b) -> c.searchAnalyzer( searchAnalyzerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep norms(Norms norms) {
		fieldModelContributor.add( (c, b) -> c.norms( norms ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep termVector(TermVector termVector) {
		fieldModelContributor.add( (c, b) -> c.termVector( termVector ) );
		return thisAsS();
	}

	private static StringIndexFieldTypeOptionsStep<?> castIndexFieldTypeOptionsStep(
			StandardIndexFieldTypeOptionsStep<?,?> optionsStep) {
		if ( optionsStep instanceof StringIndexFieldTypeOptionsStep ) {
			return (StringIndexFieldTypeOptionsStep<?>) optionsStep;
		}
		else {
			throw log.invalidFieldEncodingForFullTextFieldMapping(
					optionsStep, StringIndexFieldTypeOptionsStep.class
			);
		}
	}

}
