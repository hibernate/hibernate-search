/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl;

import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class LongitudeProcessor implements PropertyMappingAnnotationProcessor<Longitude> {

	@Override
	public void process(PropertyMappingStep mapping, Longitude annotation,
			PropertyMappingAnnotationProcessorContext context) {
		mapping.marker( GeoPointBinder.longitude().markerSet( annotation.markerSet() ) );
	}

}
