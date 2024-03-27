/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class IntArrayElementExtractor implements ContainerExtractor<int[], Integer> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_INT;
	}

	@Override
	public <T, C2> void extract(int[] container, ValueProcessor<T, ? super Integer, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		for ( int element : container ) {
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}
}
