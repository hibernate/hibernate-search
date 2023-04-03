/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.BitSet;

import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

public interface PojoTypeIndexingPlan {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 * When in doubt, you should rather use
	 * {@link #addOrUpdate(Object, DocumentRoutesDescriptor, Object, boolean, boolean, BitSet)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param providedRoutes The route to the current index shard.
	 * Only required if custom routing is enabled
	 * and the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * the routes will be computed using that bridge instead,
	 * and provided routes will be ignored.
	 * @param entity The entity to add to the index.
	 */
	void add(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Consider an entity updated,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
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
	 * @param dirtyPaths The paths to consider dirty, as a {@link BitSet}.
	 * You can build such a {@link BitSet} by obtaining the
	 * {@link org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector#dirtyFilter(PojoPathFilter) dirty filter}
	 * for the entity type and calling one of the {@code filter} methods.
	 */
	void addOrUpdate(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Entities to reindex as a result of this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If the provided ID is {@code null},
	 * Hibernate Search will attempt to extract the ID from the entity (which must be non-{@code null} in that case).
	 * @param providedRoutes The routes to the current and previous index shards.
	 * Only required if custom routing is enabled
	 * and {@code entity} is null,
	 * or the {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is missing
	 * or unable to provide all the correct previous routes.
	 * If a {@link org.hibernate.search.mapper.pojo.bridge.RoutingBridge} is assigned to the entity type,
	 * and {@code entity} is non-null,
	 * the routes will be computed using that bridge instead,
	 * and provided routes (current and previous) will all be appended to the generated "previous routes".
	 * @param entity The entity to delete from the index. May be {@code null} if {@code providedId} is non-{@code null}.
	 * @throws IllegalArgumentException If both {@code providedId} and {@code entity} are {@code null}.
	 */
	void delete(Object providedId, DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Consider an entity added, updated, or deleted,
	 * depending on the result of loading it by ID,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
	 *
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
	 * @param dirtyPaths The paths to consider dirty, as a {@link BitSet}.
	 * You can build such a {@link BitSet} by obtaining the
	 * {@link org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector#dirtyFilter(PojoPathFilter) dirty filter}
	 * for the entity type and calling one of the {@code filter} methods.
	 */
	void addOrUpdateOrDelete(Object providedId, DocumentRoutesDescriptor providedRoutes,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths);

	/**
	 * Consider an association updated with the given entities removed,
	 * and perform reindexing of the relevant entities on the inverse side of that association as necessary.
	 * <p>
	 * This is mostly useful for cases where callers do not receive update events for associations on both sides,
	 * to have Hibernate Search act "as if" the inverse side of the association had been updated.
	 * <p>
	 * <strong>WARNING:</strong> Getters returning the current state of the association on the removed entities
	 * are still expected to return the updated state of the association (for example through lazy-loading).
	 * Failing that, reindexing will index out-of-date information.
	 *
	 * @param dirtyAssociationPaths The association paths to consider dirty, as a {@link BitSet}.
	 * You can build such a {@link BitSet} by obtaining the
	 * {@link org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector#dirtyContainingAssociationFilter(PojoPathFilter) dirty association filter}
	 * for the entity type and calling one of the {@code filter} methods.
	 * @param oldState The old state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 * @param newState The new state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 */
	void updateAssociationInverseSide(BitSet dirtyAssociationPaths, Object[] oldState, Object[] newState);
}
