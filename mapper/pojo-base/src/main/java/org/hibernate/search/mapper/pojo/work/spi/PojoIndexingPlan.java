/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for indexing entities in the context of a session in a POJO mapper.
 * <p>
 * This class is stateful: it queues operations internally to apply them at a later time.
 * <p>
 * When {@link #process()} is called,
 * the entities will be processed and index documents will be built
 * and stored in an internal buffer.
 * <p>
 * When {@link #execute()} is called,
 * the operations will be actually sent to the index.
 * <p>
 * Note that {@link #execute()} will implicitly trigger processing of documents that weren't processed yet,
 * if any, so calling {@link #process()} is not necessary if you call {@link #execute()} just next.
 * <p>
 * Implementations may not be thread-safe.
 */
public interface PojoIndexingPlan {

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * Shorthand for {@code add(null, entity)}; see {@link #add(Object, Object)}.
	 *
	 * @param entity The entity to add to the index.
	 */
	void add(Object entity);

	/**
	 * Add an entity to the index, assuming that the entity is absent from the index.
	 * <p>
	 * <strong>Note:</strong> depending on the backend, this may lead to errors or duplicate entries in the index
	 * if the entity was actually already present in the index before this call.
	 * When in doubt, you should rather use {@link #addOrUpdate(Object, Object)} or {@link #addOrUpdate(Object)}.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to add to the index.
	 */
	void add(Object providedId, Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Shorthand for {@code update(null, entity)}; see {@link #addOrUpdate(Object, Object)}.
	 *
	 * @param entity The entity to update in the index.
	 */
	void addOrUpdate(Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 */
	void addOrUpdate(Object providedId, Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index,
	 * but try to avoid reindexing if the given dirty paths
	 * are known not to impact the indexed form of that entity.
	 * <p>
	 * Assumes that the entity may already be present in the index.
	 * <p>
	 * Shorthand for {@code update(null, entity, dirtyPaths)}; see {@link #addOrUpdate(Object, Object)}.
	 *
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 * ("directEntityProperty.nestedPropery").
	 */
	void addOrUpdate(Object entity, String... dirtyPaths);

	/**
	 * Update an entity in the index, or add it if it's absent from the index,
	 * but try to avoid reindexing if the given dirty paths
	 * are known not to impact the indexed form of that entity.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 * ("directEntityProperty.nestedPropery").
	 */
	void addOrUpdate(Object providedId, Object entity, String... dirtyPaths);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * Shorthand for {@code delete(null, entity)}; see {@link #delete(Object, Object)}.
	 *
	 * @param entity The entity to delete from the index.
	 */
	void delete(Object entity);

	/**
	 * Delete an entity from the index.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to delete from the index.
	 */
	void delete(Object providedId, Object entity);

	/**
	 * Purge an entity from the index.
	 * <p>
	 * Entities to reindex as a result from this operation will not be resolved.
	 * <p>
	 * No effect on the index if the entity is not in the index.
	 *
	 * @param clazz The type of the entity.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 */
	void purge(Class<?> clazz, Object providedId);

	/**
	 * Extract all data from objects passed to the indexing plan so far,
	 * create documents to be indexed and put them into an internal buffer,
	 * without writing them to the indexes.
	 * <p>
	 * In particular, ensure that all data is extracted from the POJOs
	 * and converted to the backend-specific format.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * will perform the processing if necessary.
	 */
	void process();

	/**
	 * Write all pending changes to the index now,
	 * without waiting for a Hibernate ORM flush event or transaction commit,
	 * and clear the plan so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed when all the works are complete.
	 */
	CompletableFuture<?> execute();

	/**
	 * Discard all plans of indexing.
	 */
	void discard();

	/**
	 * Discard all plans of indexing, except for parts that were already {@link #process() processed}.
	 */
	void discardNotProcessed();

}
