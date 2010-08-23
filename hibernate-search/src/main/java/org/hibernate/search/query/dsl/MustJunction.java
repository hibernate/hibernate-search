package org.hibernate.search.query.dsl;

/**
 * Represents the context in which a must clause is described.
 *  
 * @author Emmanuel Bernard
 */
public interface MustJunction extends BooleanJunction<MustJunction> {
	/**
	 * Negate the must clause.
	 * Results of the boolean query do NOT match the subquery.
	 */
	BooleanJunction not();
}
