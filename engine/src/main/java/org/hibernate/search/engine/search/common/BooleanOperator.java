/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
