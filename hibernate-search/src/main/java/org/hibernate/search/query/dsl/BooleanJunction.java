package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * Represents a boolean query that can contains one or more elements to join
 *
 * @author Emmanuel Bernard
 */
public interface BooleanJunction<T extends BooleanJunction> extends QueryCustomization<T>, Termination {
	/**
	 * The boolean query results should match the subquery
	 */
	BooleanJunction should(Query query);

	/**
	 * The boolean query results must (or must not) match the subquery
	 * Call the .not() method to ensure results of the boolean query do NOT match the subquery.
	 */
	MustJunction must(Query query);
}
