/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.spi;

/**
 * A search sort contributor, i.e. an object that will push search sorts to a collector.
 *
 * @param <C> The type of sort collector this contributor will contribute to.
 * This type is backend-specific.
 */
public interface SearchSortContributor<C> {

	/**
	 * Add zero or more sorts to the given collector.
	 *
	 * @param collector The collector to push search sorts to.
	 */
	void contribute(C collector);

}
