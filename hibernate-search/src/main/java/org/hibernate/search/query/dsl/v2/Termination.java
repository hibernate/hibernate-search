package org.hibernate.search.query.dsl.v2;

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
