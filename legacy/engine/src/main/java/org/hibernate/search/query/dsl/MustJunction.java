/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * Represents the context in which a must clause is described.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface MustJunction extends BooleanJunction<MustJunction> {
	/**
	 * Negate the must clause.
	 * Results of the boolean query do NOT match the subquery.
	 * A negated must clause always disables scoring on the subquery.
	 *
	 * @return the same {@link BooleanJunction} for method chaining.
	 */
	BooleanJunction not();

	/**
	 * Disables scoring on the subquery.
	 * If you are only interested to use this clause as a filtering criteria
	 * and don't need it to affect result scoring, this might improve performance.
	 * @return the same {@link BooleanJunction} for method chaining.
	 */
	BooleanJunction disableScoring();
}
