/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.work;

import jakarta.persistence.Entity;

/**
 * An interface for indexing entities in the context of an ORM Session.
 * <p>
 * This class is stateful: it queues operations internally to apply them at a later time.
 * <p>
 * When {@link #process()} is called,
 * or when {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED indexing listeners are enabled}
 * and a Hibernate ORM Session {@code flush()} happens,
 * the entities will be processed and index documents will be built
 * and stored in an internal buffer.
 * <p>
 * When {@link #execute()} is called,
 * or when {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED indexing listeners are enabled}
 * and a Hibernate ORM transaction is committed or a Hibernate ORM Session {@code flush()} happens outside of any transaction,
 * the operations will be actually sent to the index.
 * <p>
 * Note that {@link #execute()} will implicitly trigger processing of documents that weren't processed yet,
 * if any, so calling {@link #process()} is not necessary if you call {@link #execute()} just next.
 * <p>
 * {@link #process()} and {@link #execute()} are mostly useful when listener-triggered indexing is disabled,
 * to control the indexing process explicitly.
 */
public interface SearchIndexingPlan {

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
	 * @param providedRoutingKey The routing key to route the purge request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled or does not rely on custom routing keys.
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed directly
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}).
	 */
	void purge(Class<?> entityClass, Object providedId, String providedRoutingKey);

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
	 * @param entityName An entity name. See {@link Entity#name()}.
	 * @param providedId A value to extract the document ID from.
	 * Generally the expected value is the entity ID, but a different value may be expected depending on the mapping.
	 * @param providedRoutingKey The routing key to route the purge request to the appropriate index shard.
	 * Leave {@code null} if sharding is disabled or does not rely on custom routing keys.
	 * @throws org.hibernate.search.util.common.SearchException If the entity type is not indexed directly
	 * ({@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}).
	 */
	void purge(String entityName, Object providedId, String providedRoutingKey);

	/**
	 * Extract all data from objects passed to the indexing plan so far,
	 * creates documents to be indexed and put them into an internal buffer,
	 * without writing them to the indexes.
	 * <p>
	 * Calling this method is optional: the {@link #execute()} method
	 * or the automatic write on transaction commit will perform the extraction as necessary.
	 * <p>
	 * However, calling this method can be useful before a session is cleared
	 * if {@link org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings#INDEXING_LISTENERS_ENABLED indexing listeners} are disabled:
	 * it will make sure the data lost when clearing the session will no longer be necessary for indexing.
	 * <p>
	 * Caution: calling this method repeatedly without a call to {@link #execute()}
	 * will add more and more data to an internal document buffer,
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
