package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface TermContext extends QueryCustomization<TermContext> {
	/**
	 * field / property the term query is executed on
	 */
	TermMatchingContext onField(String field);

	TermMatchingContext onFields(String... field);

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
}
