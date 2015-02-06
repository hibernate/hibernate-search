/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl;

/**
 * @author Sascha Grebe
 */
public interface GroupingTopGroupsContext {

	/**
	 * The number of maximum groups to be return. This value needs to be greater than 0.
	 *
	 * @param topGroupCount
	 * @return
	 */
	GroupingParameterContext topGroupCount(int topGroupCount);

}
