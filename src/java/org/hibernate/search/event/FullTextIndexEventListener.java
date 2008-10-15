//$Id$
package org.hibernate.search.event;

import java.io.Serializable;

import org.slf4j.Logger;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.EntityEntry;
import org.hibernate.event.AbstractCollectionEvent;
import org.hibernate.event.AbstractEvent;
import org.hibernate.event.Destructible;
import org.hibernate.event.Initializable;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.impl.EventSourceTransactionContext;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;

/**
 * This listener supports setting a parent directory for all generated index files.
 * It also supports setting the analyzer class to be used.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Mattias Arbin
 */
//TODO work on sharing the same indexWriters and readers across a single post operation...
//TODO implement and use a LockableDirectoryProvider that wraps a DP to handle the lock inside the LDP
//TODO make this class final as soon as FullTextIndexCollectionEventListener is removed.
@SuppressWarnings( "serial" )
public class FullTextIndexEventListener implements PostDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener,
		PostCollectionRecreateEventListener, PostCollectionRemoveEventListener,
		PostCollectionUpdateEventListener, Initializable, Destructible {

	private static final Logger log = LoggerFactory.make();

	protected boolean used;
	protected SearchFactoryImplementor searchFactoryImplementor;

	/**
	 * Initialize method called by Hibernate Core when the SessionFactory starts
	 */

	public void initialize(Configuration cfg) {
		searchFactoryImplementor = ContextHolder.getOrBuildSearchFactory( cfg );
		String indexingStrategy = searchFactoryImplementor.getIndexingStrategy();
		if ( "event".equals( indexingStrategy ) ) {
			used = searchFactoryImplementor.getDocumentBuilders().size() != 0;
		}
		else if ( "manual".equals( indexingStrategy ) ) {
			used = false;
		}
	}

	public SearchFactoryImplementor getSearchFactoryImplementor() {
		return searchFactoryImplementor;
	}

	public void onPostDelete(PostDeleteEvent event) {
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( event.getEntity().getClass() ) ) {
			processWork( event.getEntity(), event.getId(), WorkType.DELETE, event );
		}
	}

	public void onPostInsert(PostInsertEvent event) {
		if ( used ) {
			final Object entity = event.getEntity();
			DocumentBuilder<Object> builder = searchFactoryImplementor.getDocumentBuilders().get( entity.getClass() );
			//not strictly necessary but a small optimization
			if ( builder != null ) {
				Serializable id = event.getId();
				processWork( entity, id, WorkType.ADD, event );
			}
		}
	}

	public void onPostUpdate(PostUpdateEvent event) {
		if ( used ) {
			final Object entity = event.getEntity();
			//not strictly necessary but a small optimization
			DocumentBuilder<Object> builder = searchFactoryImplementor.getDocumentBuilders().get( entity.getClass() );
			if ( builder != null ) {
				Serializable id = event.getId();
				processWork( entity, id, WorkType.UPDATE, event );
			}
		}
	}

	protected void processWork(Object entity, Serializable id, WorkType workType, AbstractEvent event) {
		Work work = new Work( entity, id, workType );
		final EventSourceTransactionContext transactionContext = new EventSourceTransactionContext( event.getSession() );
		searchFactoryImplementor.getWorker().performWork( work, transactionContext );
	}

	public void cleanup() {
		searchFactoryImplementor.close();
	}

	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		processCollectionEvent( event );
	}

	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		processCollectionEvent( event );
	}

	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		processCollectionEvent( event );
	}

	protected void processCollectionEvent(AbstractCollectionEvent event) {
		Object entity = event.getAffectedOwnerOrNull();
		if ( entity == null ) {
			//Hibernate cannot determine every single time the owner especially in case detached objects are involved
			// or property-ref is used
			//Should log really but we don't know if we're interested in this collection for indexing
			return;
		}
		if ( used && searchFactoryImplementor.getDocumentBuilders().containsKey( entity.getClass() ) ) {
			Serializable id = getId( entity, event );
			if ( id == null ) {
				log.warn(
						"Unable to reindex entity on collection change, id cannot be extracted: {}",
						event.getAffectedOwnerEntityName()
				);
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
}
