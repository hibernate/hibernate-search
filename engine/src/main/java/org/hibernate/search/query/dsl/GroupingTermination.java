package org.hibernate.search.query.dsl;

import org.hibernate.search.query.grouping.GroupingRequest;

public interface GroupingTermination {
	
	/**
	 * Create the final grouping request.
	 * @return The grouping request.
	 */
	GroupingRequest createGroupingRequest();
	
}
