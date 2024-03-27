/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class FloatArrayElementExtractor implements ContainerExtractor<float[], Float> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_FLOAT;
	}

	@Override
	public <T, C2> void extract(float[] container, ValueProcessor<T, ? super Float, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		for ( float element : container ) {
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}
}
