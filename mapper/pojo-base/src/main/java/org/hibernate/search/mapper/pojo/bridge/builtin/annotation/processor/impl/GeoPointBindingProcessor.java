/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public class GeoPointBindingProcessor
		implements TypeMappingAnnotationProcessor<GeoPointBinding>,
		PropertyMappingAnnotationProcessor<GeoPointBinding> {
	@Override
	public void process(TypeMappingStep mapping, GeoPointBinding annotation,
			TypeMappingAnnotationProcessorContext context) {
		mapping.binder( createBinder( annotation ) );
	}

	@Override
	public void process(PropertyMappingStep mapping, GeoPointBinding annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.binder( createBinder( annotation ) );
	}

	private GeoPointBinder createBinder(GeoPointBinding annotation) {
		return GeoPointBinder.create()
				.fieldName( annotation.fieldName() )
				.markerSet( annotation.markerSet() )
				.projectable( annotation.projectable() )
				.sortable( annotation.sortable() );
	}
}
