/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.extractor.builtin.impl;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;

public class MapValueExtractor<T> extends AbstractIteratorBasedElementExtractor<Map<?, T>, T> {
	@Override
	public String toString() {
		return BuiltinContainerExtractors.MAP_VALUE;
	}

	@Override
	protected Iterator<T> iterator(Map<?, T> container) {
		return container.values().iterator();
	}
}
