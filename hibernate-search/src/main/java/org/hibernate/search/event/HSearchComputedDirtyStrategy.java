package org.hibernate.search.event;

import java.io.Serializable;

import org.hibernate.event.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Emmanuel Bernard
 */
class HSearchComputedDirtyStrategy implements DirtyStrategy, Serializable {
	
	public String[] getDirtyPropertyNames(PostUpdateEvent event) {
		EntityPersister persister = event.getPersister();
		Object[] oldState = event.getOldState();
		if ( oldState != null ) {
			int[] dirtyProperties = persister.findDirty(
					event.getState(), oldState, event.getEntity(), event.getSession()
			);
			String[] propertyNames = persister.getPropertyNames();
			int length = dirtyProperties.length;
			String[] dirtyPropertyNames = new String[length];
			for ( int i = 0; i < length; i++ ) {
				dirtyPropertyNames[i] = propertyNames[dirtyProperties[i]];
			}
			return dirtyPropertyNames;
		}
		else {
			return null;
		}
	}
	
}
