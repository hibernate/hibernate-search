/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.OptionalLong;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class OptionalLongValueExtractor implements ContainerExtractor<OptionalLong, Long> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.OPTIONAL_LONG;
	}

	@Override
	public <T, C2> void extract(OptionalLong container, ValueProcessor<T, ? super Long, C2> perValueProcessor, T target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		if ( container.isPresent() ) {
			perValueProcessor.process( target, container.getAsLong(), context, extractionContext );
		}
	}

	@Override
	public boolean multiValued() {
		return false;
	}
}
