/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource;

import java.io.Serializable;

/**
 * Represents an indexing and querying work load.
 * A workload can be batched by calling the startBatch / commitBatch methods.
 * In this case, all indexing operations happen at the end of the batch
 * and all entities loaded have identity equality guarantees.
 * The latter means that if two ids are equal, then the two instances are ==.
 *
 * A batch can be cancelled with the cancelBatch method.
 *
 * Not thread-safe.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface WorkLoad {

	// Batch boundaries methods

	/**
	 * Starts a batch.
	 *
	 * @throws java.lang.IllegalStateException if a batch was already started
	 */
	void startBatch();

	/**
	 * Commits a batch and execute all indexing operations.
	 * The entity cache is discarded.
	 *
	 * @throws java.lang.IllegalStateException if a batch was not started
	 */
	void commitBatch();

	/**
	 * Cancels a batch. All indexing operations are discarded.
	 * The entity cache is discarded.
	 *
	 * @throws java.lang.IllegalStateException if a batch was not started
	 */
	void endBatch();

	// entity indexing and querying methods
	// TODO should we make the difference between add, update vs index and delete vs purge?

	/**
	 * Create a fulltext query on top of a native Lucene query returning the matching objects
	 * of type <code>entities</code> and their respective subclasses.
	 *
	 * @param luceneQuery The native Lucene query to be run against the Lucene index.
	 * @param entities List of classes for type filtering. The query result will only return entities of
	 * the specified types and their respective subtype. If no class is specified no type filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> wrapping around the native Lucene query.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object.
	 * Indexation is batched per transaction: if a transaction is active, the operation
	 * will not affect the index at least until commit.
	 *
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces an index operation.
	 *
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 */
	<T> void index(T entity);

	/**
	 * @return the <code>SearchFactory</code> instance.
	 */
	SearchFactory getSearchFactory();

	/**
	 * Remove the entity with the type <code>entityType</code> and the identifier <code>id</code> from the index.
	 * If <code>id == null</code> all indexed entities of this type and its indexed subclasses are deleted. In this
	 * case this method behaves like {@link #purgeAll(Class)}.
	 *
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces a purge operation.
	 *
	 * @param entityType The type of the entity to delete.
	 * @param id The id of the entity to delete.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	<T> void purge(Class<T> entityType, Serializable id);

	/**
	 * Remove all entities from of particular class and all its subclasses from the index.
	 *
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity type will be ignored.
	 *
	 * @param entityType The class of the entities to remove.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	<T> void purgeAll(Class<T> entityType);

	// TODO should we add the mass indexer?
}
