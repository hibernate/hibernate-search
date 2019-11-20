/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for indexing entities in the context of a session in a POJO mapper,
 * immediately, asynchronously and without any sort of {@link PojoIndexingPlan planning}.
 */
public interface PojoIndexer {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> add(Object providedId, Object entity);

}
