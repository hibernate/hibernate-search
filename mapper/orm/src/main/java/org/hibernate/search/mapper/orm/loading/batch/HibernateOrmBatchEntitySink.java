/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.batch;

import java.util.List;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A sink for use by a {@link HibernateOrmBatchEntityLoader}.
 *
 * @param <E> The type of loaded entities.
 */
@Incubating
public interface HibernateOrmBatchEntitySink<E> {

	/**
	 * Adds a batch of entities to the sink.
	 * <p>
	 * The list and entities need to stay usable at least until this method returns,
	 * as they will be consumed synchronously.
	 * Afterwards, they can be discarded or reused at will.
	 *
	 * @param batch The next batch of identifiers. Never {@code null}, never empty.
	 */
	void accept(List<? extends E> batch);

}
