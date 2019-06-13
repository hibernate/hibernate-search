/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

// TODO HSEARCH-3069 Also document what happens when a transaction is *not* active
/**
 * An interface for writing to indexes in the context of an ORM Session.
 * <p>
 * When a transaction is active, operations are queued internally,
 * and are only applied on transaction commit.
 * <p>
 * {@link #process()} and {@link #execute()} allow to control explicitly
 * how operations are applied, without relying on transaction commits,
 * which can be useful when indexing large amounts of data in batches.
 */
public interface SearchSessionWritePlan {

	/**
	 * Add or update a document in the index if the entity type is mapped to an index
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}),
	 * and re-index documents that embed this entity
	 * (through {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded} for example).
	 *
	 * @param entity The entity to add or update in the index.
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed,
	 * neither directly ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed})
	 * nor through another indexed type that embeds it ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded} for example).
	 */
	void addOrUpdate(Object entity);

	/**
	 * Delete the entity from the index if the entity type is mapped to an index
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}),
	 * and re-index documents that embed this entity
	 * (through {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded} for example).
	 *
	 * @param entity The entity to delete from the index.
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed,
	 * neither directly ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed})
	 * nor through another indexed type that embeds it ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded} for example).
	 */
	void delete(Object entity);

	/**
	 * Delete the entity from the index.
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
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed directly
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}).
	 */
	void purge(Class<?> entityClass, Object providedId);

	/**
	 * Extract all data from objects passed to the writer so far,
	 * without writing to the indexes.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * or the automatic write on transaction commit will perform the extraction as necessary.
	 * <p>
	 * However, calling this method can be useful before a session is cleared:
	 * it will make sure the data lost when clearing the session will no longer be necessary for indexing.
	 * <p>
	 * Caution: calling this method repeatedly without a call to {@link #execute()}
	 * will add more and more data to an internal write buffer,
	 * which may lead to an {@link OutOfMemoryError}.
	 * Use with caution in batch processes.
	 */
	void process();

	/**
	 * Write all pending changes to the index now,
	 * without waiting for a Hibernate ORM flush event or transaction commit.
	 * <p>
	 * If a transaction is active and is ultimately rolled back,
	 * the written changes will not be rolled back,
	 * causing indexes to become out of sync with the database.
	 * Thus, calling this method should generally be avoided,
	 * and relying on automatic write on transaction commit should be preferred.
	 */
	void execute();

}
