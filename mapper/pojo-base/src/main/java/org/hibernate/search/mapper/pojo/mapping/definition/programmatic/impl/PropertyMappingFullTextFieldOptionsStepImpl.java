/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.bridge.binding.spi.FieldModelContributorContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoIndexMappingCollectorPropertyNode;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;


class PropertyMappingFullTextFieldOptionsStepImpl
		extends AbstractPropertyMappingStandardFieldOptionsStep<PropertyMappingFullTextFieldOptionsStep>
		implements PropertyMappingFullTextFieldOptionsStep {

	PropertyMappingFullTextFieldOptionsStepImpl(PropertyMappingStep parent, String relativeFieldName) {
		super( parent, relativeFieldName, FieldModelContributorContext::stringTypeOptionsStep );
	}

	private boolean contributeDefaultAnalyzer = true;

	@Override
	PropertyMappingFullTextFieldOptionsStep thisAsS() {
		return this;
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep analyzer(String analyzerName) {
		contributeDefaultAnalyzer = false;
		fieldModelContributor.add( c -> c.stringTypeOptionsStep().analyzer( analyzerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep searchAnalyzer(String searchAnalyzerName) {
		fieldModelContributor.add( c -> c.stringTypeOptionsStep().searchAnalyzer( searchAnalyzerName ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep norms(Norms norms) {
		fieldModelContributor.add( c -> c.stringTypeOptionsStep().norms( norms ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep termVector(TermVector termVector) {
		fieldModelContributor.add( c -> c.stringTypeOptionsStep().termVector( termVector ) );
		return thisAsS();
	}

	@Override
	public PropertyMappingFullTextFieldOptionsStep highlightable(Collection<Highlightable> highlightable) {
		fieldModelContributor.add( c -> c.stringTypeOptionsStep().highlightable( highlightable ) );
		return thisAsS();
	}

	@Override
	public void contributeIndexMapping(PojoIndexMappingCollectorPropertyNode collector) {
		if ( contributeDefaultAnalyzer ) {
			analyzer( AnalyzerNames.DEFAULT );
		}

		super.contributeIndexMapping( collector );
	}
}
