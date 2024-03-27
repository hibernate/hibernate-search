/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class OptionalValueExtractor<T> implements ContainerExtractor<Optional<T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.OPTIONAL;
	}

	@Override
	public <T1, C2> void extract(Optional<T> container, ValueProcessor<T1, ? super T, C2> perValueProcessor, T1 target,
			C2 context, ContainerExtractionContext extractionContext) {
		if ( container == null ) {
			return;
		}
		if ( container.isPresent() ) {
			perValueProcessor.process( target, container.get(), context, extractionContext );
		}
	}

	@Override
	public boolean multiValued() {
		return false;
	}
}
