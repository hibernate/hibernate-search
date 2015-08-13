/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
	 * @param query the query to match
	 * @return a {@link BooleanJunction}
	 */
	BooleanJunction should(Query query);

	/**
	 * The boolean query results must (or must not) match the subquery
	 * Call the .not() method to ensure results of the boolean query do NOT match the subquery.
	 *
	 * @param query the query to match
	 * @return a {@link MustJunction}
	 */
	MustJunction must(Query query);

	/**
	 * @return true if no restrictions have been applied
	 */
	boolean isEmpty();
}
