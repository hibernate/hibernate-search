/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

public enum RegexpQueryFlag {

	/**
	 * Enables {@code INTERVAL} operator ({@code <>})
	 */
	INTERVAL,
	/**
	 * Enables {@code INTERSECTION} operator ({@code &})
	 */
	INTERSECTION,
	/**
	 * Enables {@code ANY_STRING} operator ({@code @})
	 */
	ANY_STRING

}
