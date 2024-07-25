/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import java.util.OptionalLong;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A loader for mass loading of entity identifiers, used in particular during mass indexing.
 * <p>
 * If the total count of identifiers to load is unknown, {@link #totalCount()} should remain unimplemented,
 * i.e. return an empty optional. This will let the mass indexer know to increment the total on each new batch loaded instead
 * of relying on the total being provided before the indexing starts.
 * <p>
 * The loading of the identifiers is considered as finished when {@link MassIdentifierSink#complete()} is called
 * from the {@link #loadNext()}.
 */
@Incubating
public interface MassIdentifierLoader extends AutoCloseable {

	/**
	 * Closes this {@link MassIdentifierLoader}.
	 */
	@Override
	void close();

	/**
	 * Loads one batch of identifiers and adds them to the sink,
	 * or calls {@link MassIdentifierSink#complete()}
	 * to notify the caller that there are no more identifiers to load.
	 * <p>
	 * Calls to the sink must be performed synchronously (before this method returns).
	 * @throws InterruptedException If the thread was interrupted while performing I/O operations.
	 * This will lead to aborting mass indexing completely.
	 */
	void loadNext() throws InterruptedException;

	/**
	 * @return The total count of identifiers expected to be loaded.
	 */
	default OptionalLong totalCount() {
		return OptionalLong.empty();
	}
}
