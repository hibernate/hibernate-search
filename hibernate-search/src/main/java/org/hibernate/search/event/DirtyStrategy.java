package org.hibernate.search.event;

import org.hibernate.event.PostUpdateEvent;

/**
 * Return the list of dirty properties
 *
 * @author Emmanuel Bernard
 */
interface DirtyStrategy {
	/**
	 * Returns the names of the dirty properties
	 */
	String[] getDirtyPropertyNames(PostUpdateEvent postUpdateEvent);
}
