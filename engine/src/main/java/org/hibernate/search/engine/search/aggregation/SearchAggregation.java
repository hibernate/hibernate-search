/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation;

/**
 * A search aggregation, i.e. a way to turn search query hits into one or more summarizing metric(s).
 * <p>
 * Implementations of this interface are provided to users by Hibernate Search.
 * Users must not try to implement this interface.
 *
 * @param <A> The type of result for this aggregation.
 */
public interface SearchAggregation<A> {
}
