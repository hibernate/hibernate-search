package org.hibernate.search.query.dsl.v2;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public interface TermContext extends QueryCustomization<TermContext> {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext onField(String field);

	TermMatchingContext onFields(String... field);

}
