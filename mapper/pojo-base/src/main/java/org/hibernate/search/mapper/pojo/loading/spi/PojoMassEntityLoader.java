/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;

/**
 * A loader for mass loading of entities, used in particular during mass indexing.
 * <p>
 * Compared to {@link PojoSelectionEntityLoader}, this loader:
 * <ul>
 *     <li>Receives batches of identifiers from a {@link PojoMassIdentifierLoader}</li>
 *     <li>Is expected to load a very large number of entities in multiple small batches.</li>
 *     <li>Pushes loaded entities to a sink.</li>
 *     <li>Sets up its own context (session, transactions, ...), instead of potentially relying on a pre-existing context.</li>
 *     <li>Is free to discard the entities after the sink is done processing them.</li>
 * </ul>
 *
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassEntityLoader<I> extends AutoCloseable {

	/**
	 * Closes this {@link PojoMassEntityLoader}.
	 */
	@Override
	void close();

	/**
	 * Loads the entities corresponding to the given identifiers and adds them to the given sink,
	 * blocking the current thread while doing so.
	 * <p>
	 * Calls to the sink must be performed synchronously (before this method returns).
	 * <p>
	 * Entities must be passed to the sink using a single call to {@link PojoMassEntitySink#accept(List)}.
	 * <p>
	 * Entities passed to the sink do not need to be the same order as {@code identifiers}.
	 *
	 * @param identifiers A list of identifiers of entities to load.
	 * @throws InterruptedException If the thread was interrupted while performing I/O operations.
	 * This will lead to aborting mass indexing completely.
	 */
	void load(List<I> identifiers) throws InterruptedException;

}
