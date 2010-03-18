package org.hibernate.search.query.dsl.v2;

/**
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * build a term query
	 */
	TermContext term();


	/**
	 * Boolean query
	 */
	BooleanJunction<BooleanJunction> bool();
}
