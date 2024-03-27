/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Iterator;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class IterableElementExtractor<T> extends AbstractIteratorBasedElementExtractor<Iterable<T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.ITERABLE;
	}

	@Override
	protected Iterator<T> iterator(Iterable<T> container) {
		return container.iterator();
	}
}
