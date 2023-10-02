/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations.impl;

import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Deprecated
public class LatitudeAnnotationProcessor implements PropertyMappingAnnotationProcessor<Latitude> {

	@Override
	public void process(PropertyMappingStep mapping, Latitude annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.marker( GeoPointBinder.latitude().markerSet( annotation.of() ) );
	}

}
