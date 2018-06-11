/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

import java.util.concurrent.CompletableFuture;

/**
 * A worker that accumulates works in a list (called a changeset),
 * and executes them only when {@link #execute()} is called.
 * <p>
 * Relative ordering of works within a changeset will be preserved.
 * <p>
 * Implementations may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface ChangesetPojoWorker extends PojoWorker {

	/**
	 * Update an entity in the index, or add it if it's absent from the index,
	 * but try to avoid reindexing if the given dirty paths
	 * are known not to impact the indexed form of that entity.
	 * <p>
	 * Assumes that the entity may already be present in the index.
	 * <p>
	 * Shorthand for {@code update(null, entity, dirtyPaths)}; see {@link #update(Object, Object)}.
	 *
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 * ("directEntityProperty.nestedPropery").
	 */
	void update(Object entity, String... dirtyPaths);

	/**
	 * Update an entity in the index, or add it if it's absent from the index,
	 * but try to avoid reindexing if the given dirty paths
	 * are known not to impact the indexed form of that entity.
	 *
	 * @param id The provided ID for the entity.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 * @param dirtyPaths The paths to consider dirty, formatted using the dot-notation
	 * ("directEntityProperty.nestedPropery").
	 */
	void update(Object id, Object entity, String... dirtyPaths);

	/**
	 * Prepare the changeset execution, i.e. execute as much as possible without writing to the index.
	 * <p>
	 * In particular, ensure that all data is extracted from the POJOs
	 * and converted to the backend-specific format.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * will perform the preparation if necessary.
	 */
	void prepare();

	/**
	 * Start executing all the works in this changeset, and clear the changeset so that it can be re-used.
	 *
	 * @return A {@link CompletableFuture} that will be completed when all the works are complete.
	 */
	CompletableFuture<?> execute();

}
