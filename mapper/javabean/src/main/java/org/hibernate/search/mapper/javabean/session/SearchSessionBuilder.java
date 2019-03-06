/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session;


public interface SearchSessionBuilder {

	/**
	 * @param tenantId The tenant ID to use when performing index-related operatiosn (indexing, searching, ...)
	 * in the resulting session.
	 * @return {@code this} for method chaining.
	 */
	SearchSessionBuilder tenantId(String tenantId);

	/**
	 * @return The resulting session.
	 */
	SearchSession build();

}
