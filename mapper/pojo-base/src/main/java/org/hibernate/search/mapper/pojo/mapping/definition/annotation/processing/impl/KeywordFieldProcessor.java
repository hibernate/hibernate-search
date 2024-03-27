/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class KeywordFieldProcessor extends AbstractNonFullTextFieldAnnotationProcessor<KeywordField> {

	@Override
	PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			KeywordField annotation, String fieldName) {
		PropertyMappingKeywordFieldOptionsStep fieldContext = mappingContext.keywordField( fieldName );

		String normalizer = annotation.normalizer();
		if ( !normalizer.isEmpty() ) {
			fieldContext.normalizer( annotation.normalizer() );
		}

		Norms norms = annotation.norms();
		if ( !Norms.DEFAULT.equals( norms ) ) {
			fieldContext.norms( norms );
		}

		return fieldContext;
	}

	@Override
	String getName(KeywordField annotation) {
		return annotation.name();
	}

	@Override
	Projectable getProjectable(KeywordField annotation) {
		return annotation.projectable();
	}

	@Override
	Searchable getSearchable(KeywordField annotation) {
		return annotation.searchable();
	}

	@Override
	Sortable getSortable(KeywordField annotation) {
		return annotation.sortable();
	}

	@Override
	Aggregable getAggregable(KeywordField annotation) {
		return annotation.aggregable();
	}

	@Override
	String getIndexNullAs(KeywordField annotation) {
		return annotation.indexNullAs();
	}

	@Override
	ValueBridgeRef getValueBridge(KeywordField annotation) {
		return annotation.valueBridge();
	}

	@Override
	ValueBinderRef getValueBinder(KeywordField annotation) {
		return annotation.valueBinder();
	}

	@Override
	ContainerExtraction getExtraction(KeywordField annotation) {
		return annotation.extraction();
	}
}
