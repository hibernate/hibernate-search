/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class CollectionElementExtractor<T> extends AbstractIteratorBasedElementExtractor<Collection<T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.COLLECTION;
	}

	@Override
	protected Iterator<T> iterator(Collection<T> container) {
		return container.iterator();
	}
}
