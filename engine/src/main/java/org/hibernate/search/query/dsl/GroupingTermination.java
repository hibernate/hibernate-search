/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl;

import org.hibernate.search.query.grouping.GroupingRequest;

/**
 * @author Sascha Grebe
 */
public interface GroupingTermination {

	/**
	 * Create the final grouping request.
	 *
	 * @return The grouping request.
	 */
	GroupingRequest createGroupingRequest();

}
