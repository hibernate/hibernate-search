/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine;

/**
 * Interface to implement boost values as functions
 * of the object value being boosted.
 * Implementations must be threadsafe.
 *
 * @author Hardy Ferentschik
 * @see org.hibernate.search.annotations.Boost
 */
public interface BoostStrategy {

	float defineBoost(Object value);

}
