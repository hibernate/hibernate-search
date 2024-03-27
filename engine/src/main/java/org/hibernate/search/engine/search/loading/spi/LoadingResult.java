/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.loading.spi;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;

/**
 * The result of the loading of the entities by the {@link ProjectionHitMapper}.
 *
 * @param <E> The type of entities.
 */
public interface LoadingResult<E> {

	/**
	 * @param key The key that was previously returned by {@link ProjectionHitMapper#planLoading(DocumentReference)}.
	 * @return The loaded entity corresponding to the key, or {@code null} if the entity could not be loaded.
	 */
	E get(Object key);

	/**
	 * Convert a document reference to the reference specific to the mapper.
	 *
	 * @param reference The document reference.
	 * @return The reference specific to the mapper.
	 */
	EntityReference convertReference(DocumentReference reference);

}
