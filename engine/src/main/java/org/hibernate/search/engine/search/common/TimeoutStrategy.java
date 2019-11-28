/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common;

public enum TimeoutStrategy {

	/**
	 * With this strategy, partial results will be returned
	 * whether and when a time out event occurs.
	 */
	LIMIT_FETCHING,

	/**
	 * With this strategy, an exception will be raised
	 * whether and when a time out event occurs.
	 */
	RAISE_AN_EXCEPTION

}
