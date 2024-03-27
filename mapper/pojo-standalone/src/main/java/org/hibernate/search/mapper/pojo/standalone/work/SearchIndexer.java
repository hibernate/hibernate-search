/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.work;

import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An interface for indexing entities in the context of a session.
 * <p>
 * This class is stateless: operations start executing as soon as they are submitted.
 * <p>
 * This contract is thread-safe.
 */
@Incubating
public interface SearchIndexer {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code add(null, null, entity)}; see {@link #add(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> add(Object entity) {
		return add( null, null, entity );
	}

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code add(providedId, null, entity)}; see {@link #add(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> add(Object providedId, Object entity) {
		return add( providedId, null, entity );
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
	 * @param providedRoutes The route to the current index shard.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 *
	 * @param entityClass The class of the entity to add to the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The route to the current index shard.
	 * Leave {@code null} if sharding is disabled or if you don't use a custom {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> add(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code addOrUpdate(null, null, entity)}; see {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> addOrUpdate(Object entity) {
		return addOrUpdate( null, null, entity );
	}

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code addOrUpdate(providedId, null, entity)}; see {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> addOrUpdate(Object providedId, Object entity) {
		return addOrUpdate( providedId, null, entity );
	}

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to update in the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 *
	 * @param entityClass The class of the entity to update in the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * Leave {@code null} if sharding is disabled or if you don't use a custom {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> addOrUpdate(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code delete(null, null, entity)}; see {@link #delete(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> delete(Object entity) {
		return delete( null, entity );
	}

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * Shorthand for {@code delete(providedId, null, entity)}; see {@link #delete(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to delete from the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	default CompletionStage<?> delete(Object providedId, Object entity) {
		return delete( providedId, null, entity );
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
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @param entity The entity to delete from the index.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Purge an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param entityClass The class of the entity to delete from the index.
	 * @param providedId A value to extract the document ID from.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Leave {@code null} if sharding is disabled
	 * or to have Hibernate Search compute the value through the assigned {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge}.
	 * @return A {@link CompletionStage} reflecting the completion state of the operation.
	 */
	CompletionStage<?> delete(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

}
