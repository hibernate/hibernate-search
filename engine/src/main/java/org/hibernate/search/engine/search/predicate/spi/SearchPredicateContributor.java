/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

/**
 * A search predicate contributor, i.e. an object that will push search predicates to a collector.
 *
 * @param <CTX> The type of the context passed to the {@link #contribute(Object, Object)} method.
 * @param <C> The type of predicate collector this contributor will contribute to.
 * These types are backend-specific.
 */
public interface SearchPredicateContributor<CTX, C> {

	/**
	 * Add zero or more predicates to the given collector.
	 *
	 * @param context The context in which the predicates are registered.
	 * @param collector The collector to push search predicates to.
	 */
	void contribute(CTX context, C collector);

}
