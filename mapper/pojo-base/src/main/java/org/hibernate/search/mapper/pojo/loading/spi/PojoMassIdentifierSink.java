/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;

/**
 * A sink for use by a {@link PojoMassIdentifierLoader}.
 *
 * @param <I> The type of entity identifiers.
 */
public interface PojoMassIdentifierSink<I> {

	/**
	 * Adds a batch of identifiers to the sink.
	 * <p>
	 * Identifiers can be passed in any order,
	 * but the caller must ensure that a given identifier is never passed more than once,
	 * even across multiple calls to this method.
	 * <p>
	 * The list only needs to stay usable until this method returns, as it will be copied.
	 * Afterwards, it can be discarded or reused at will.
	 * The identifiers themselves, however, must not change after this method is called,
	 * because they will be consumed asynchronously.
	 *
	 * @param batch The next batch of identifiers. Never {@code null}, never empty.
	 * @throws InterruptedException If the thread was interrupted while handling the batch.
	 * This exception should generally not be caught: just propagate it.
	 */
	void accept(List<? extends I> batch) throws InterruptedException;

	/**
	 * Signals that no more identifiers are available.
	 */
	void complete();

}
