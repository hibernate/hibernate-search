/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class FullTextFieldProcessor extends PropertyStandardFieldAnnotationProcessor<FullTextField> {

	@Override
	PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(PropertyMappingStep mappingContext,
			FullTextField annotation, String fieldName) {
		PropertyMappingFullTextFieldOptionsStep fieldContext = mappingContext.fullTextField( fieldName )
				.analyzer( annotation.analyzer() );

		if ( !annotation.searchAnalyzer().isEmpty() ) {
			fieldContext.searchAnalyzer( annotation.searchAnalyzer() );
		}

		Norms norms = annotation.norms();
		if ( !Norms.DEFAULT.equals( norms ) ) {
			fieldContext.norms( norms );
		}

		TermVector termVector = annotation.termVector();
		if ( !TermVector.DEFAULT.equals( termVector ) ) {
			fieldContext.termVector( termVector );
		}

		return fieldContext;
	}

	@Override
	String getName(FullTextField annotation) {
		return annotation.name();
	}

	@Override
	Projectable getProjectable(FullTextField annotation) {
		return annotation.projectable();
	}

	@Override
	Searchable getSearchable(FullTextField annotation) {
		return annotation.searchable();
	}

	@Override
	ValueBridgeRef getValueBridge(FullTextField annotation) {
		return annotation.valueBridge();
	}

	@Override
	ValueBinderRef getValueBinder(FullTextField annotation) {
		return annotation.valueBinder();
	}

	@Override
	ContainerExtraction getExtraction(FullTextField annotation) {
		return annotation.extraction();
	}
}
