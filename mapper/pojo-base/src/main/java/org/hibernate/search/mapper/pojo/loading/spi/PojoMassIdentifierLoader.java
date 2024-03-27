/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

/**
 * A loader of entity identifiers in batch, used in particular for mass indexing.
 */
public interface PojoMassIdentifierLoader extends AutoCloseable {

	/**
	 * Closes this {@link PojoMassIdentifierLoader}.
	 */
	@Override
	void close();

	/**
	 * @return The total count of identifiers expected to be loaded.
	 */
	long totalCount();

	/**
	 * Loads one batch of identifiers and adds them to the sink,
	 * or calls {@link PojoMassIdentifierSink#complete()}
	 * to notify the caller that there are no more identifiers to load.
	 * <p>
	 * Calls to the sink must be performed synchronously (before this method returns).
	 * @throws InterruptedException If the thread was interrupted while performing I/O operations.
	 * This will lead to aborting mass indexing completely.
	 */
	void loadNext() throws InterruptedException;

}
