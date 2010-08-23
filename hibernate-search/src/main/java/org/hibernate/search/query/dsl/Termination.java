package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface Termination<T> {
	/**
	 * Return the lucene query representing the operation
	 */
	Query createQuery();
}
