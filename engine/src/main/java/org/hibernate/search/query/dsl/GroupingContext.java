/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl;

public interface GroupingContext {

	/**
	 * Group by the given field name.
	 * 
	 * @param fieldName The field name.
	 * @return The parameter context.
	 */
	GroupingTopGroupsContext onField(String fieldName);

}
