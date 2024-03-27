/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.List;

/**
 * A sink for use by a {@link PojoMassIdentifierLoader}.
 *
 * @param <E> The type of loaded entities.
 */
public interface PojoMassEntitySink<E> {

	/**
	 * Adds a batch of entities to the sink.
	 * <p>
	 * The list and entities need to stay usable at least until this method returns,
	 * as they will be consumed synchronously.
	 * Afterwards, they can be discarded or reused at will.
	 *
	 * @param batch The next batch of identifiers. Never {@code null}, never empty.
	 * @throws InterruptedException If the thread was interrupted while handling the batch.
	 * This exception should generally not be caught: just propagate it.
	 */
	void accept(List<? extends E> batch) throws InterruptedException;

}
