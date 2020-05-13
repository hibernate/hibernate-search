/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.work;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for indexing entities in the context of a session.
 * <p>
 * This class is stateless: operations start executing as soon as they are submitted.
 * <p>
 * This contract is thread-safe.
 */
public interface SearchIndexer {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code add(null, entity)}; see {@link #add(Object, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	default CompletableFuture<?> add(Object entity) {
		return add( null, entity );
	}

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
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

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code addOrUpdate(null, entity)}; see {@link #addOrUpdate(Object, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	default CompletableFuture<?> addOrUpdate(Object entity) {
		return addOrUpdate( null, entity );
	}

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> addOrUpdate(Object providedId, Object entity);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code delete(null, entity)}; see {@link #delete(Object, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	default CompletableFuture<?> delete(Object entity) {
		return delete( null, entity );
	}

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to delete from the index.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> delete(Object providedId, Object entity);

	/**
	 * Purge an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param entityClass The class of the entity to delete from the index.
	 * @param providedId A value to extract the document ID from.
	 * @param providedRoutingKey The routing key to route the purge request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled or if you don't use a custom {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> purge(Class<?> entityClass, Object providedId, String providedRoutingKey);

}
