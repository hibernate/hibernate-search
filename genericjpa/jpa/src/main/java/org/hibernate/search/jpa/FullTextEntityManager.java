/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jpa;

import javax.persistence.EntityManager;
import java.io.Serializable;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.genericjpa.batchindexing.MassIndexer;

/**
 * Extends an EntityManager with Full-Text operations
 *
 * @author Emmanuel Bernard
 * @author Martin Braun
 */
public interface FullTextEntityManager extends EntityManager {

	/**
	 * Create a fulltext query on top of a native Lucene query returning the matching objects of columnTypes
	 * <code>entities</code> and their respective subclasses.
	 *
	 * @param luceneQuery The native Lucene query to be run against the Lucene index.
	 * @param entities List of classes for columnTypes filtering. The query result will only return entities of the specified
	 * types and their respective subtype. If no class is specified no columnTypes filtering will take place.
	 *
	 * @return A <code>FullTextQuery</code> wrapping around the native Lucene query.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with
	 * <code>@Indexed</code>.
	 */
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class<?>... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object. Indexation is batched per search-transaction: if a
	 * transaction is active, the operation will not affect the index at least until commit.
	 *
	 * @param entity The entity to index - must not be <code>null</code>.
	 *
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	<T> void index(T entity);

	/**
	 * @return the <code>SearchFactory</code> instance.
	 */
	SearchFactory getSearchFactory();

	/**
	 * Remove the entity with the columnTypes <code>entityType</code> and the identifier <code>id</code> from the index. If
	 * <code>id == null</code> all indexed entities of this columnTypes and its indexed subclasses are deleted. In this case
	 * this method behaves like {@link #purgeAll(Class)}.
	 *
	 * @param entityType The columnTypes of the entity to delete.
	 * @param id The id of the entity to delete.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with
	 * <code>@Indexed</code>.
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	<T> void purge(Class<T> entityType, Serializable id);

	/**
	 * Remove all entities from of particular class and all its subclasses from the index.
	 *
	 * @param entityType The class of the entities to remove.
	 *
	 * @throws IllegalArgumentException if entityType is <code>null</code> or not a class or superclass annotated with
	 * <code>@Indexed</code>.
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	<T> void purgeAll(Class<T> entityType);

	<T> void purgeByTerm(Class<T> entityType, String field, Integer val);

	<T> void purgeByTerm(Class<T> entityType, String field, Long val);

	<T> void purgeByTerm(Class<T> entityType, String field, Float val);

	<T> void purgeByTerm(Class<T> entityType, String field, Double val);

	<T> void purgeByTerm(Class<T> entityType, String field, String val);

	/**
	 * Flush all index changes forcing Hibernate Search to apply all changes to the index not waiting for the batch
	 * limit.
	 *
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	void flushToIndexes();

	/**
	 * <b>different from the original Hibernate Search!</b> <br>
	 * <br>
	 * this has to be called when you want to change the index manually!
	 *
	 * @throws IllegalStateException if a search-transaction is already in progress
	 */
	void beginSearchTransaction();

	/**
	 * <b>different from the original Hibernate Search!</b> <br>
	 * <br>
	 * this has to be called when you want to change the index manually!
	 *
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	void rollbackSearchTransaction();

	/**
	 * <b>different from the original Hibernate Search!</b> <br>
	 * <br>
	 * this has to be called when you want to change the index manually!
	 *
	 * @throws IllegalStateException if no search-transaction is in progress
	 */
	void commitSearchTransaction();

	boolean isSearchTransactionInProgress();

	/**
	 * @throws IllegalStateException if search-transaction is still in progress. underlying EntityManager is still
	 * closed.
	 */
	void close();

	/**
	 * Creates a MassIndexer to rebuild the indexes of some or all indexed entity types. Instances cannot be reused. Any
	 * {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor} registered on the entity types are
	 * applied: each instance will trigger an
	 * {@link org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor#onAdd(Object)} event from where you can
	 * customize the indexing operation.
	 *
	 * @param types optionally restrict the operation to selected types
	 *
	 * @return a new MassIndexer
	 */
	MassIndexer createIndexer(Class<?>... types);

}
