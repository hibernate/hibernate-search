// $Id$
package org.hibernate.search.event;

import java.util.Properties;

import org.hibernate.event.EventListeners;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods initializing Hibernate Search event listeners.
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class EventListenerRegister {

	private static final Logger log = LoggerFactory.getLogger(EventListenerRegister.class);

	@SuppressWarnings("unchecked")
	public static void enableHibernateSearch(EventListeners eventListeners, Properties properties) {		
		// check whether search is explicitly enabled - if so there is nothing to do		
		String enableSearchListeners = properties.getProperty( "hibernate.search.autoregister_listeners" );
		if("false".equalsIgnoreCase(enableSearchListeners )) {
			log.info("Property hibernate.search.autoregister_listeners is set to false." +
					" No attempt will be made to register Hibernate Search event listeners.");
			return;
		}
				
		FullTextIndexEventListener searchEventListener = new FullTextIndexEventListener();
		
		//TODO Generalize this. Pretty much the same code all the time. Reflection? 
		final Class<? extends FullTextIndexEventListener> searchEventListenerClass = searchEventListener.getClass();
		{
			boolean present = false;
			PostInsertEventListener[] listeners = eventListeners
					.getPostInsertEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener.getClass()
							|| searchEventListenerClass == eventListener.getClass().getSuperclass(); //for FullTextIndexCollectionEventListener
				}
				if (!present) {
					int length = listeners.length + 1;
					PostInsertEventListener[] newListeners = new PostInsertEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostInsertEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostInsertEventListeners(new PostInsertEventListener[] { searchEventListener });
			}
		}
		{
			boolean present = false;
			PostUpdateEventListener[] listeners = eventListeners
					.getPostUpdateEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener.getClass()
							|| searchEventListenerClass == eventListener.getClass().getSuperclass(); //for FullTextIndexCollectionEventListener
				}
				if (!present) {
					int length = listeners.length + 1;
					PostUpdateEventListener[] newListeners = new PostUpdateEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostUpdateEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostUpdateEventListeners(new PostUpdateEventListener[] { searchEventListener });
			}
		}
		{
			boolean present = false;
			PostDeleteEventListener[] listeners = eventListeners
					.getPostDeleteEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener.getClass()
							|| searchEventListenerClass == eventListener.getClass().getSuperclass(); //for FullTextIndexCollectionEventListener
				}
				if (!present) {
					int length = listeners.length + 1;
					PostDeleteEventListener[] newListeners = new PostDeleteEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostDeleteEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostDeleteEventListeners(new PostDeleteEventListener[] { searchEventListener });
			}
		}		
		{
			boolean present = false;
			PostCollectionRecreateEventListener[] listeners = eventListeners.getPostCollectionRecreateEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener.getClass()
							|| searchEventListenerClass == eventListener.getClass().getSuperclass(); //for FullTextIndexCollectionEventListener
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionRecreateEventListener[] newListeners = new PostCollectionRecreateEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostCollectionRecreateEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionRecreateEventListeners(
						new PostCollectionRecreateEventListener[] { searchEventListener }
				);
			}
		}
		{
			boolean present = false;
			PostCollectionRemoveEventListener[] listeners = eventListeners.getPostCollectionRemoveEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener.getClass()
							|| searchEventListenerClass == eventListener.getClass().getSuperclass(); //for FullTextIndexCollectionEventListener
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionRemoveEventListener[] newListeners = new PostCollectionRemoveEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostCollectionRemoveEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionRemoveEventListeners(
						new PostCollectionRemoveEventListener[] { searchEventListener }
				);
			}
		}
		{
			boolean present = false;
			PostCollectionUpdateEventListener[] listeners = eventListeners.getPostCollectionUpdateEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present || searchEventListenerClass == eventListener.getClass();
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionUpdateEventListener[] newListeners = new PostCollectionUpdateEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = searchEventListener;
					eventListeners.setPostCollectionUpdateEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionUpdateEventListeners(
						new PostCollectionUpdateEventListener[] { searchEventListener }
				);
			}
		}		
	}
}
