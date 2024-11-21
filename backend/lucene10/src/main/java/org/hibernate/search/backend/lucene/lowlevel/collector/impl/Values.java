/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;

/**
 * A generic accessor to per-document values.
 *
 * @param <T> The type of values.
 */
public interface Values<T> {

	/**
	 * Sets the context to use for the next calls to {@link #get(int)}.
	 * @param context A {@link LeafReaderContext}.
	 * @throws IOException If an underlying I/O operation fails.
	 */
	void context(LeafReaderContext context) throws IOException;

	/**
	 * @return The value for the given document in the current leaf.
	 * @throws IOException If an underlying I/O operation fails.
	 */
	T get(int doc) throws IOException;

}
