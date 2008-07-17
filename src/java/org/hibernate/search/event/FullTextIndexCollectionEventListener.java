// $Id$
package org.hibernate.search.event;

import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;

/**
 * @author Emmanuel Bernard
 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener}
 */
@SuppressWarnings("serial")
@Deprecated 
public class FullTextIndexCollectionEventListener extends FullTextIndexEventListener
		implements PostCollectionRecreateEventListener,
		PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener {

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostRecreateCollection(PostCollectionRecreateEvent)}
	 */
	@Deprecated 	
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostRemoveCollection(PostCollectionRemoveEvent)}
	 */
	@Deprecated 	
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	/**
	 * @deprecated As of release 3.1.0, replaced by {@link FullTextIndexEventListener#onPostUpdateCollection(PostCollectionUpdateEvent)}
	 */
	@Deprecated 	
	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}
}
