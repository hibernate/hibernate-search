/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.util.Collection;

import org.apache.lucene.search.Collector;

/**
 * Tagging interface for collector keys.
 * <p>
 * This is used for de-duplication of collectors, to avoid collecting the same data twice during the same search.
 *
 * @param <C> The type of collector.
 * @param <T> The type of the results produced after merging multiple collectors via {@link org.apache.lucene.search.CollectorManager#reduce(Collection)}
 */
public interface CollectorKey<C extends Collector, T> {

	static <C extends Collector, T> CollectorKey<C, T> create() {
		return new CollectorKey<C, T>() {
		};
	}

}
