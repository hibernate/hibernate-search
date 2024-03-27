/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class BooleanArrayElementExtractor implements ContainerExtractor<boolean[], Boolean> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ARRAY_BOOLEAN;
	}

	@Override
	public <T, C2> void extract(boolean[] container, ValueProcessor<T, ? super Boolean, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		for ( boolean element : container ) {
			perValueProcessor.process( target, element, context, extractionContext );
		}
	}
}
