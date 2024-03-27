/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class DoubleArrayElementExtractor implements ContainerExtractor<double[], Double> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_DOUBLE;
	}

	@Override
	public <T, C2> void extract(double[] container, ValueProcessor<T, ? super Double, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		for ( double element : container ) {
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}
}
