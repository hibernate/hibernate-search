/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping;

/**
 * A entry point to execute works on POJO-mapped indexes.
 * <p>
 * Implementations may not be thread-safe.
 *
 * @author Yoann Rodiere
 */
public interface PojoWorker {

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
	 * When in doubt, you should rather use {@link #update(Object, Object)} or {@link #update(Object)}.
	 *
	 * @param id The provided ID for the entity.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to add to the index.
	 */
	void add(Object id, Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 * <p>
	 * Shorthand for {@code update(null, entity)}; see {@link #update(Object, Object)}.
	 *
	 * @param entity The entity to update in the index.
	 */
	void update(Object entity);

	/**
	 * Update an entity in the index, or add it if it's absent from the index.
	 *
	 * @param id The provided ID for the entity.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to update in the index.
	 */
	void update(Object id, Object entity);

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
	 * @param id The provided ID for the entity.
	 * If {@code null}, Hibernate Search will attempt to extract the ID from the entity.
	 * @param entity The entity to delete from the index.
	 */
	void delete(Object id, Object entity);

}
