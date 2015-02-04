package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.GroupingContext;
import org.hibernate.search.query.dsl.GroupingTopGroupsContext;

public class ConnectedGroupingContext implements GroupingContext {

	private final GroupingBuildingContext context;
	
	public ConnectedGroupingContext(GroupingBuildingContext context) {
		this.context = context;
	}
	
	@Override
	public GroupingTopGroupsContext onField(String fieldName) {
		context.setFieldName(fieldName);
		return new ConnectedGroupingTopGroupsContext(context);
	}

}
