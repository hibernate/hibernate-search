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
import org.hibernate.event.AbstractCollectionEvent;
import org.hibernate.search.backend.WorkType;
import org.hibernate.engine.EntityEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	private static Log log = LogFactory.getLog( FullTextIndexCollectionEventListener.class );

	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	private void processCollectionEvent(AbstractCollectionEvent event) {
		Object entity = event.getAffectedOwnerOrNull();
		if ( entity == null ) {
			//Hibernate cannot determine every single time the owner especially incase detached objects are involved
			// or property-ref is used
			//Should log really but we don't know if we're interested in this collection for indexing
			return;
		}
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( entity.getClass() ) ) {
			Serializable id = getId( entity, event );
			if (id == null) {
				log.warn( "Unable to reindex entity on collection change, id cannot be extracted: " + event.getAffectedOwnerEntityName() );
				return;
			}
			processWork( entity, id, WorkType.COLLECTION, event );
		}
	}

	private Serializable getId(Object entity, AbstractCollectionEvent event) {
		Serializable id = event.getAffectedOwnerIdOrNull();
		if ( id == null ) {
			//most likely this recovery is unnecessary since Hibernate Core probably try that 
			EntityEntry entityEntry = event.getSession().getPersistenceContext().getEntry( entity );
			id = entityEntry == null ? null : entityEntry.getId();
		}
		return id;
	}

	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}
}
