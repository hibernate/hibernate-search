/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.facet;

/**
 * @author Hardy Ferentschik
 */
public interface RangeFacet<T> extends Facet {
	/**
	 * @return the lower boundary of this range
	 */
	T getMin();

	/**
	 * @return the upper boundary of this range
	 */
	T getMax();

	/**
	 * @return {@code true} if the lower boundary is included in the range, {@code false} otherwise
	 */
	boolean isIncludeMin();

	/**
	 * @return {@code true} if the upper boundary is included in the range, {@code false} otherwise
	 */
	boolean isIncludeMax();
}
