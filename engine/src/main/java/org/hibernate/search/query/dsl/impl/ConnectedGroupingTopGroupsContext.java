package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.GroupingParameterContext;
import org.hibernate.search.query.dsl.GroupingTopGroupsContext;

public class ConnectedGroupingTopGroupsContext implements GroupingTopGroupsContext {

	private final GroupingBuildingContext context;
	
	public ConnectedGroupingTopGroupsContext(GroupingBuildingContext context) {
		this.context = context;
	}
	
	@Override
	public GroupingParameterContext topGroupCount(int topGroupCount) {
		context.setTopGroupCount(topGroupCount);
		return new ConnectedGroupingParameterContext(context);
	}
	
}
