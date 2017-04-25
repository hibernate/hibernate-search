/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.engine.spi.QueryDescriptor;

/**
 * Extends an EntityManager with Full-Text operations
 *
 * @author Emmanuel Bernard
 */
public interface FullTextEntityManager extends EntityManager {

	/**
	 * Create a fulltext query on top of a native Lucene query returning the matching objects
	 * of type <code>entities</code> and their respective subclasses.
	 *
	 * @param luceneQuery The native Lucene query to be rn against the Lucene index.
	 * @param entities List of classes for type filtering. The query result will only return entities of
	 * the specified types and their respective subtype. If no class is specified no type filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> wrapping around the native Lucene wuery.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	/**
	 * Creates a fulltext query from the given query descriptor.
	 *
	 * @param descriptor The query descriptor
	 * @param entities List of classes for type filtering. The query result will only return entities of
	 * the specified types and their respective subtype. If no class is specified no type filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> using the given query descriptor.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	FullTextQuery createFullTextQuery(QueryDescriptor descriptor, Class<?>... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object.
	 * Indexation is batched per transaction: if a transaction is active, the operation
	 * will not affect the index at least until commit.
	 * <p>
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces an index operation.
	 *
	 * @param <T> the type of the entity to index
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
	 * <p>
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity will be ignored:
	 * this method forces a purge operation.
	 *
	 * @param <T> the type of the entity to purge
	 * @param entityType The type of the entity to delete.
	 * @param id The id of the entity to delete.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	<T> void purge(Class<T> entityType, Serializable id);

	/**
	 * Remove all entities from of particular class and all its subclasses from the index.
	 * <p>
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity type will be ignored.
	 *
	 * @param <T> the type of the entity to purge
	 * @param entityType The class of the entities to remove.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with <code>@Indexed</code>.
	 */
	<T> void purgeAll(Class<T> entityType);

	/**
	 * Flush all index changes forcing Hibernate Search to apply all changes to the index not waiting for the batch limit.
	 */
	void flushToIndexes();

	/**
	 * Creates a MassIndexer to rebuild the indexes of some
	 * or all indexed entity types.
	 * Instances cannot be reused.
	 * <p>
	 * Any {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity types are applied: each instance will trigger
	 * an {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor#onAdd(Object)} event from where you can customize the indexing operation.
	 *
	 * @param types optionally restrict the operation to selected types
	 * @return a new MassIndexer
	 */
	MassIndexer createIndexer(Class<?>... types);

}
