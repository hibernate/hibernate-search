package org.hibernate.search.filter;

import org.hibernate.search.FullTextFilter;

/**
 * @author Emmanuel Bernard
 */
public interface FullTextFilterImplementor extends FullTextFilter {
	/**
	 * Returns the Filter name
	 */
	String getName();

	//TODO should we expose Map<String, Object> getParameters()
}
