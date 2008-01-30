//$
package org.hibernate.search.event;

import java.io.Serializable;

import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.AbstractEvent;
import org.hibernate.search.backend.WorkType;

/**
 * Support collection event listening (starts from hibernate core 3.2.6)
 * FIXME deprecate as soon as we target Core 3.3 and merge back into the superclass
 *
 * @author Emmanuel Bernard
 */
public class FullTextIndexCollectionEventListener extends FullTextIndexEventListener
		implements PostCollectionRecreateEventListener,
		PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener {
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		Object entity = event.getAffectedOwner();
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( entity.getClass() ) ) {
			processWork( entity, getId( entity, event ), WorkType.COLLECTION, event );
		}
	}

	private Serializable getId(Object entity, AbstractEvent event) {
		return event.getSession().getPersistenceContext().getEntry( entity ).getId();
	}

	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		Object entity = event.getAffectedOwner();
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( entity.getClass() ) ) {
			processWork( entity, getId( entity, event ), WorkType.COLLECTION, event );
		}
	}

	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		Object entity = event.getAffectedOwner();
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( entity.getClass() ) ) {
			processWork( entity, getId( entity, event ), WorkType.COLLECTION, event );
		}
	}
}
