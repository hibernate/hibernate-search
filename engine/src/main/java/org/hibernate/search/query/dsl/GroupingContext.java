package org.hibernate.search.query.dsl;

public interface GroupingContext {
	
	/**
	 * Group by the given field name.
	 * @param fieldName The field name.
	 * @return The parameter context.
	 */
	GroupingTopGroupsContext onField(String fieldName);
	
}
