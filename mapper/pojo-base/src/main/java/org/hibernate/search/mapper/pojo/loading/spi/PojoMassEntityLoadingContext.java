/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

/**
 * The context passed to {@link PojoMassLoadingStrategy#createEntityLoader(Set, PojoMassEntityLoadingContext)}.
 *
 * @param <E> The type of loaded entities.
 */
public interface PojoMassEntityLoadingContext<E> {

	/**
	 * @return The parent, mapper-specific loading context.
	 */
	PojoMassLoadingContext parent();

	/**
	 * @param sessionContext The session context, used to create an indexer in particular.
	 * @return A sink that the loader will add loaded entities to.
	 */
	PojoMassEntitySink<E> createSink(PojoMassIndexingSessionContext sessionContext);

	/**
	 * @return The tenant identifier to use ({@code null} if none).
	 */
	String tenantIdentifier();

}
