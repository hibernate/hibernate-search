/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

/**
 * An interface for indexing entities in the context of a session in a POJO mapper.
 * <p>
 * This class is stateful: it queues operations internally to apply them at a later time.
 * <p>
 * When {@link #process()} is called,
 * the entities will be processed and index documents will be built
 * and stored in an internal buffer.
 * <p>
 * When {@link #executeAndReport(OperationSubmitter)} is called,
 * the operations will be actually sent to the index.
 * <p>
 * Note that {@link #executeAndReport(OperationSubmitter)} will implicitly trigger processing of documents that weren't processed yet,
 * if any, so calling {@link #process()} is not necessary if you call {@link #executeAndReport(OperationSubmitter)} just next.
 * <p>
 * Implementations may not be thread-safe.
 */
public interface PojoIndexingPlan {

	/**
	 * @param typeIdentifier The identifier of the entity type.
	 * @return The indexing plan for the given entity type,
	 * or {@code null} if that type is going to be ignored by this indexing plan.
	 */
	PojoTypeIndexingPlan typeIfIncludedOrNull(PojoRawTypeIdentifier<?> typeIdentifier);

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 * When in doubt, you should rather use
	 * {@link #addOrUpdate(PojoRawTypeIdentifier, Object, DocumentRoutesDescriptor, Object, boolean, boolean, BitSet)}.
	 *
	 * @param typeIdentifier The identifier of the entity type.
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
	 * @deprecated Use {@code typeIfIncludedOrNull(typeIdentifier)} instead, then (if non-null) {@code .add(...)} on the result.
	 */
	@Deprecated
	void add(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Consider an entity updated,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
	 *
	 * @param typeIdentifier The identifier of the entity type.
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
	 * @deprecated Use {@code typeIfIncludedOrNull(typeIdentifier)} instead, then (if non-null) {@code .addOrUpdate(...)} on the result.
	 */
	@Deprecated
	void addOrUpdate(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths);

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
	 * @deprecated Use {@code typeIfIncludedOrNull(typeIdentifier)} instead, then (if non-null) {@code .delete(...)} on the result.
	 */
	@Deprecated
	void delete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes, Object entity);

	/**
	 * Consider an entity added, updated, or deleted,
	 * depending on the result of loading it by ID,
	 * and perform reindexing of this entity as well as containing entities as necessary,
	 * taking into account {@code dirtyPaths}, {@code forceSelfDirty} and {@code forceContainingDirty}.
	 *
	 * @param typeIdentifier The identifier of the entity type.
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
	 * @deprecated Use {@code typeIfIncludedOrNull(typeIdentifier)} instead, then (if non-null) {@code .addOrUpdateOrDelete(...)} on the result.
	 */
	@Deprecated
	void addOrUpdateOrDelete(PojoRawTypeIdentifier<?> typeIdentifier, Object providedId,
			DocumentRoutesDescriptor providedRoutes,
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
	 * @param typeIdentifier The identifier of the entity type on one side of the association.
	 * @param dirtyAssociationPaths The association paths to consider dirty, as a {@link BitSet}.
	 * You can build such a {@link BitSet} by obtaining the
	 * {@link org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeExtendedMappingCollector#dirtyContainingAssociationFilter(PojoPathFilter) dirty association filter}
	 * for the entity type and calling one of the {@code filter} methods.
	 * @param oldState The old state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 * @param newState The new state of the entity whose associations are dirty.
	 * May be {@code null}, in which case this state will not yield any reindexing.
	 * @deprecated Use {@code typeIfIncludedOrNull(typeIdentifier)} instead, then (if non-null) {@code .updateAssociationInverseSide(...)} on the result.
	 */
	@Deprecated
	void updateAssociationInverseSide(PojoRawTypeIdentifier<?> typeIdentifier,
			BitSet dirtyAssociationPaths, Object[] oldState, Object[] newState);

	/**
	 * Extract all data from objects passed to the indexing plan so far,
	 * create documents to be indexed and put them into an internal buffer,
	 * without writing them to the indexes.
	 * <p>
	 * In particular, ensure that all data is extracted from the POJOs
	 * and converted to the backend-specific format.
	 * <p>
	 * Calling this method is optional: the {@link #executeAndReport(OperationSubmitter)} method
	 * will perform the processing if necessary.
	 */
	void process();

	/**
	 * Write all pending changes to the index now,
	 * without waiting for a Hibernate ORM flush event or transaction commit,
	 * and clear the plan so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed with an execution report when all the works are complete.
	 */
	CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter);

	/**
	 * Discard all plans of indexing.
	 */
	void discard();

	/**
	 * Discard all plans of indexing, except for parts that were already {@link #process() processed}.
	 */
	void discardNotProcessed();

}
