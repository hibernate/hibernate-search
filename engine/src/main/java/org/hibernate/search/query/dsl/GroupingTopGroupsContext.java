package org.hibernate.search.query.dsl;

public interface GroupingTopGroupsContext {

	/**
	 * The number of maximum groups to be return. This value needs to be greater than 0.
	 * @param topGroupCount
	 * @return
	 */
	GroupingParameterContext topGroupCount(int topGroupCount);
	
}
