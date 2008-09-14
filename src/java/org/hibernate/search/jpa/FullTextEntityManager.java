// $Id$
package org.hibernate.search.jpa;

import java.io.Serializable;
import javax.persistence.EntityManager;

import org.hibernate.search.SearchFactory;

/**
 * Extends an EntityManager with Full-Text operations
 *
 * @author Emmanuel Bernard
 */
public interface FullTextEntityManager extends EntityManager {
	/**
	 * Create a Query on top of a native Lucene Query returning the matching objects
	 * of type <code>entities</code> and their respective subclasses.
	 * If no entity is provided, no type filtering is done.
	 */
	FullTextQuery createFullTextQuery(org.apache.lucene.search.Query luceneQuery, Class... entities);

	/**
	 * Force the (re)indexing of a given <b>managed</b> object.
	 * Indexation is batched per transaction: if a transaction is active, the operation
	 * will not affect the index at least until commit.
	 * 
	 * @throws IllegalArgumentException if entity is null or not an @Indexed entity
	 */
	void index(Object entity);

	/**
	 * return the SearchFactory
	 */
	SearchFactory getSearchFactory();
	/**
	 * Remove a particular entity from a particular class of an index.
	 *
	 * @param entityType
	 * @param id
	 *
	 * @throws IllegalArgumentException if entityType is null or not an @Indexed entity type
	 */
	public void purge(Class entityType, Serializable id);

	/**
	 * Remove all entities from a particular class of an index.
	 *
	 * @param entityType
	 *
	 * @throws IllegalArgumentException if entityType is null or not an @Indexed entity type
	 */
	public void purgeAll(Class entityType);

	/**
	 * flush index change
	 * Force Hibernate Search to apply all changes to the index no waiting for the batch limit
	 */
	public void flushToIndexes();

}
