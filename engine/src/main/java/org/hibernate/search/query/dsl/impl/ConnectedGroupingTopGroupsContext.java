/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.GroupingParameterContext;
import org.hibernate.search.query.dsl.GroupingTopGroupsContext;

/**
 * @author Sascha Grebe
 */
public class ConnectedGroupingTopGroupsContext implements GroupingTopGroupsContext {

	private final GroupingBuildingContext context;

	public ConnectedGroupingTopGroupsContext(GroupingBuildingContext context) {
		this.context = context;
	}

	@Override
	public GroupingParameterContext topGroupCount(int topGroupCount) {
		context.setTopGroupCount( topGroupCount );
		return new ConnectedGroupingParameterContext( context );
	}

}
