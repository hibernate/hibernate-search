/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Sort;
import org.hibernate.search.query.dsl.GroupingParameterContext;
import org.hibernate.search.query.grouping.GroupingRequest;

/**
 * @author Sascha Grebe
 */
public class ConnectedGroupingParameterContext implements GroupingParameterContext {

	private final GroupingBuildingContext context;

	public ConnectedGroupingParameterContext(GroupingBuildingContext context) {
		this.context = context;
	}

	@Override
	public GroupingRequest createGroupingRequest() {
		return context.getGroupingRequest();
	}

	@Override
	public GroupingParameterContext groupSort(Sort groupSort) {
		context.setGroupSort( groupSort );
		return this;
	}

	@Override
	public GroupingParameterContext withinGroupSort(Sort withinGroupSort) {
		context.setWithinGroupSort( withinGroupSort );
		return this;
	}

	@Override
	public GroupingParameterContext maxDocsPerGroup(int maxDocsPerGroup) {
		context.setMaxDocsPerGroup( maxDocsPerGroup );
		return this;
	}

	@Override
	public GroupingParameterContext groupOffset(int groupOffset) {
		context.setGroupOffset( groupOffset );
		return this;
	}

	@Override
	public GroupingParameterContext calculateTotalGroupCount(boolean calculateTotalGroupCount) {
		context.setCalculateTotalGroupCount( calculateTotalGroupCount );
		return this;
	}

}
