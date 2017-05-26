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
 *
 * @deprecated Index-time boosting will not be possible anymore starting from Lucene 7.
 * You should use query-time boosting instead, for instance by calling
 * {@link org.hibernate.search.query.dsl.FieldCustomization#boostedTo(float) boostedTo(float)}
 * when building queries with the Hibernate Search query DSL.
 */
@Deprecated
public interface BoostStrategy {

	float defineBoost(Object value);

}
