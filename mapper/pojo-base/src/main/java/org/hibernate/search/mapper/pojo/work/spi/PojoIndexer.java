/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

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
	 * @param providedRoutes The route to the current index shard.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to add to the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter);

	/**
	 * @see #add(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, Object, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)
	 * @deprecated Use {@link #add(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, Object, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)}
	 * instead.
	 */
	@Deprecated
	default CompletableFuture<?> add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return add(
				typeIdentifier, providedId, providedRoutes, entity, commitStrategy, refreshStrategy,
				OperationSubmitter.blocking()
		);
	}

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to update in the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter);

	/**
	 * @see #addOrUpdate(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, Object, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)
	 * @deprecated Use {@link #addOrUpdate(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, Object, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)}
	 * instead.
	 */
	@Deprecated
	default CompletableFuture<?> addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return addOrUpdate(
				typeIdentifier, providedId, providedRoutes, entity, commitStrategy, refreshStrategy,
				OperationSubmitter.blocking()
		);
	}

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
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to delete from the index.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter);

	/**
	 * @see #delete(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)
	 * @deprecated Use {@link #delete(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)}
	 * instead.
	 */
	@Deprecated
	default CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return delete(
				typeIdentifier, providedId, providedRoutes, entity, commitStrategy, refreshStrategy,
				OperationSubmitter.blocking()
		);
	}

	/**
	 * Purge an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param typeIdentifier The identifier of the entity type.
	 * @param providedId A value to extract the document ID from.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param commitStrategy How to handle the commit.
	 * @param refreshStrategy How to handle the refresh.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full
	 * @return A {@link CompletableFuture} reflecting the completion state of the operation.
	 */
	CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter);

	/**
	 * @see #delete(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)
	 * @deprecated Use {@link #delete(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, DocumentCommitStrategy, DocumentRefreshStrategy, OperationSubmitter)}
	 * instead.
	 */
	@Deprecated
	default CompletableFuture<?> delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy
	) {
		return delete(
				typeIdentifier, providedId, providedRoutes, commitStrategy, refreshStrategy,
				OperationSubmitter.blocking()
		);
	}

}
