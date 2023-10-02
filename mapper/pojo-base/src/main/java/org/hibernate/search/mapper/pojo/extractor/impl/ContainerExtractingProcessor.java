/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.impl;

import org.hibernate.search.mapper.pojo.extractor.ContainerExtractionContext;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;
import org.hibernate.search.mapper.pojo.extractor.ValueProcessor;

public final class ContainerExtractingProcessor<T, C, V, C2> implements ValueProcessor<T, C, C2> {
	private final ContainerExtractor<? super C, V> extractor;
	private final ValueProcessor<T, ? super V, C2> perValueProcessor;

	public ContainerExtractingProcessor(ContainerExtractor<? super C, V> extractor,
			ValueProcessor<T, ? super V, C2> perValueProcessor) {
		this.extractor = extractor;
		this.perValueProcessor = perValueProcessor;
	}

	@Override
	public String toString() {
		return "ContainerExtractingProcessor["
				+ "extractor=" + extractor
				+ ", perValueProcessor=" + perValueProcessor
				+ "]";
	}

	@Override
	public void process(T target, C container, C2 context, ContainerExtractionContext extractionContext) {
		extractor.extract( container, perValueProcessor, target, context, extractionContext );
	}
}
