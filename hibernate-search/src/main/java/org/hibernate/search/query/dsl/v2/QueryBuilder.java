package org.hibernate.search.query.dsl.v2;

/**
 * @author Emmanuel Bernard
 */
public interface QueryBuilder {
	/**
	 * build a term query
	 */
	TermContext exact();

	/**
	 * Use a fuzzy search approximation (aka edit distance)
	 */
	FuzzyContext fuzzy();

	/**
	 * Treat the query as a wildcard:
	 *  - ? represents any single character
	 *  - * represents any character sequence
	 * For faster results, it is recommended that the query text does not
	 * start with ? or *
	 */
	WildcardContext wildcard();

	/**
	 * Boolean query
	 */
	BooleanJunction<BooleanJunction> bool();
}
