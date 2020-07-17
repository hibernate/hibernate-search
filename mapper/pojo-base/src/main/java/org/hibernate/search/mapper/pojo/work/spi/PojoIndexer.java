/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * An interface for indexing entities in the context of a session in a POJO mapper,
 * immediately, asynchronously and without any sort of {@link PojoIndexingPlan planning}
 * or handling of containing entities.
 * <p>
 * Implementations must be thread-safe if the underlying session is thread-safe.
 */
public interface PojoIndexer {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutingKey The routing key to route the add request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
	 * @param entity The entity to add to the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutingKey The routing key to route the addOrUpdate request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
	 * @param entity The entity to update in the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutingKey The routing key to route the delete request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
	 * @param entity The entity to delete from the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	/**
	 * Purge an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * @param providedRoutingKey The routing key to route the purge request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled or if you don't use a custom {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge}.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> purge(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId, String providedRoutingKey,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

}
