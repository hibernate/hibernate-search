package org.hibernate.search.query.dsl.v2;

import org.hibernate.search.query.dsl.v2.TermContext;

/**
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * build a term query
	 */
	TermContext term();
}
