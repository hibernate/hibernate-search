//$Id$
package org.hibernate.search.jpa;

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
	 * Indexation is batched per transaction
	 */
	void index(Object entity);

	/**
	 * return the SearchFactory
	 */
	SearchFactory getSearchFactory();

}
