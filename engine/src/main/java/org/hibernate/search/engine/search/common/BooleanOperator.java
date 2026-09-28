package org.hibernate.search.engine.search.common;

public enum BooleanOperator {

	/**
	 * <em>AND</em> operator: all terms/clauses must match
	 */
	AND,
	/**
	 * <em>OR</em> operator: at least one term/clause must match.
	 */
	OR;

}
