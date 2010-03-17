package org.hibernate.search.query.dsl.v2;

/**
* @author Emmanuel Bernard
*/
public interface EntityContext {
	EntityContext overridesForField(String field, String analyzerName);

	/**
	 * return the query builder
	 */
	QueryBuilder get();
}
