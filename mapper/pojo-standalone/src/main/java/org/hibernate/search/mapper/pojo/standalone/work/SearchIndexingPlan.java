/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.work;

import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An interface for indexing entities in the context of a session.
 * <p>
 * This class is stateful: it queues operations internally to apply them when the session is closed.
 * <p>
 * This contract is not thread-safe.
 */
@Incubating
public interface SearchIndexingPlan {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Shorthand for {@code add(null, null, entity)}; see {@link #add(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 */
	void add(Object entity);

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 * When in doubt, you should rather use {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object)} or {@link #addOrUpdate(Object)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The route to the current index shard.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes will be ignored.
	 * @param entity The entity to add to the index.
	 */
	void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 * When in doubt, you should rather use {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object)} or {@link #addOrUpdate(Object)}.
	 *
	 * @param entityClass The class of the entity to add to the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The route to the current index shard.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 */
	void add(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Shorthand for {@code addOrUpdate(null, null, entity)}; see {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to update in the index.
	 */
	void addOrUpdate(Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 *  @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param entity The entity to update in the index.
	 */
	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 *
	 * @param entityClass The class of the entity to update in the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 */
	void addOrUpdate(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

	/**
	 * Consider an entity updated,
	 * and perform reindexing of this entity as well as containing entities as necessary.
	 * <p>
	 * Assumes that the entity may already be present in the index.
	 * <p>
	 * Shorthand for {@code addOrUpdate(null, null, entity, false, false, dirtyPaths)};
	 * see {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object, boolean, boolean, String...)}.
	 *
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 * ("directEntityProperty.nestedPropery").
	 */
	void addOrUpdate(Object entity, String... dirtyPaths);

	/**
	 * Consider an entity updated,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}.
	 * <p>
	 * Assumes that the entity may already be present in the index.
	 * <p>
	 * Shorthand for {@code addOrUpdate(providedId, providedRoutes, entity, false, false, dirtyPaths)};
	 * see {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object, boolean, boolean, String...)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 */
	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity, String... dirtyPaths);

	/**
	 * Consider an entity updated,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
	 * <p>
	 * Assumes that the entity may already be present in the index.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param entity The entity to update in the index.
	 * @param forceSelfDirty If {@code true}, forces reindexing of this entity regardless of the dirty paths.
	 * @param forceContainingDirty If {@code true}, forces the resolution of containing entities as dirty regardless of the dirty paths.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 */
	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, String... dirtyPaths);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Shorthand for {@code delete(null, null, entity)}; see {@link #delete(Object, DocumentRoutesDescriptor, Object)}.
	 *
	 * @param entity The entity to delete from the index.
	 */
	void delete(Object entity);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *  @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and {@code entity} is null,
	 * or the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * and {@code entity} is non-null,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param entity The entity to delete from the index.
	 */
	void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Delete the entity from the index.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 * <p>
	 * On contrary to {@link #delete(Object)},
	 * if documents embed this entity
	 * (through {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded} for example),
	 * these documents will <strong>not</strong> be re-indexed,
	 * leaving the indexes in an inconsistent state
	 * until they are re-indexed manually.
	 *
	 * @param entityClass The class of the entity to delete from the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled.
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed directly
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}).
	 */
	void purge(Class<?> entityClass, Object providedId, DocumentRoutesDescriptor providedRoutes);

	/**
	 * Consider an entity added, updated, or deleted,
	 * depending on the result of loading it by ID,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
	 *
	 * @param entityClass The class of the entity to add, update or delete from the index.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param forceSelfDirty If {@code true}, forces reindexing of this entity regardless of the dirty paths.
	 * @param forceContainingDirty If {@code true}, forces the resolution of containing entities as dirty.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 */
	void addOrUpdateOrDelete(Class<?> entityClass, Object providedId,
			DocumentRoutesDescriptor providedRoutes,
			boolean forceSelfDirty, boolean forceContainingDirty, String... dirtyPaths);

}
