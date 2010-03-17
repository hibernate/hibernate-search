package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface TermContext {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext on(String field);

}
