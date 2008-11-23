//$Id$
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.ProvidedId;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.bridge.BridgeFactory;
import org.hibernate.search.impl.InitContext;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Set up and provide a manager for indexed classes.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Sylvain Vieujot
 * @author Richard Hallier
 * @author Hardy Ferentschik
 */
public class DocumentBuilderIndexedEntity<T> extends DocumentBuilderContainedEntity<T> {

	private final DirectoryProvider[] directoryProviders;
	private final IndexShardingStrategy shardingStrategy;

	/**
	 * Constructor used on an @Indexed entity.
	 */
	public DocumentBuilderIndexedEntity(XClass clazz, InitContext context, DirectoryProvider[] directoryProviders,
						   IndexShardingStrategy shardingStrategy, ReflectionManager reflectionManager) {

		super( clazz, context, reflectionManager );

		this.entityState = EntityState.INDEXED;
		this.directoryProviders = directoryProviders;
		this.shardingStrategy = shardingStrategy;

		if ( idKeywordName == null ) {
			// if no DocumentId then check if we have a ProvidedId instead
			ProvidedId provided = findProvidedId( clazz, reflectionManager );
			if ( provided == null ) {
				throw new SearchException( "No document id in: " + clazz.getName() );
			}

			idBridge = BridgeFactory.extractTwoWayType( provided.bridge() );
			idKeywordName = provided.name();
		}
	}

	private ProvidedId findProvidedId(XClass clazz, ReflectionManager reflectionManager) {
		ProvidedId id = null;
		XClass currentClass = clazz;
		while ( id == null && ( !reflectionManager.equals( currentClass, Object.class ) ) ) {
			id = currentClass.getAnnotation( ProvidedId.class );
			currentClass = clazz.getSuperclass();
		}
		return id;
	}

	//TODO could we use T instead of EntityClass?
	public void addWorkToQueue(Class<T> entityClass, T entity, Serializable id, WorkType workType, List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		//TODO with the caller loop we are in a n^2: optimize it using a HashMap for work recognition

		List<LuceneWork> toDelete = new ArrayList<LuceneWork>();
		boolean duplicateDelete = false;
		for ( LuceneWork luceneWork : queue ) {
			//avoid unecessary duplicated work
			if ( luceneWork.getEntityClass() == entityClass
					) {
				Serializable currentId = luceneWork.getId();
				//currentId != null => either ADD or Delete work
				if ( currentId != null && currentId.equals( id ) ) { //find a way to use Type.equals(x,y)
					if ( workType == WorkType.DELETE ) { //TODO add PURGE?
						//DELETE should have precedence over any update before (HSEARCH-257)
						//if an Add work is here, remove it
						//if an other delete is here remember but still search for Add
						if ( luceneWork instanceof AddLuceneWork ) {
							toDelete.add( luceneWork );
						}
						else if ( luceneWork instanceof DeleteLuceneWork ) {
							duplicateDelete = true;
						}
					}
					else {
						//we can safely say we are out, the other work is an ADD
						return;
					}
				}
				//TODO do something to avoid multiple PURGE ALL and OPTIMIZE
			}
		}
		for ( LuceneWork luceneWork : toDelete ) {
			queue.remove( luceneWork );
		}
		if ( duplicateDelete ) {
			return;
		}

		String idInString = idBridge.objectToString( id );
		if ( workType == WorkType.ADD ) {
			Document doc = getDocument( entity, id );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc ) );
		}
		else if ( workType == WorkType.DELETE || workType == WorkType.PURGE ) {
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
		}
		else if ( workType == WorkType.PURGE_ALL ) {
			queue.add( new PurgeAllLuceneWork( entityClass ) );
		}
		else if ( workType == WorkType.UPDATE || workType == WorkType.COLLECTION ) {
			Document doc = getDocument( entity, id );
			/**
			 * even with Lucene 2.1, use of indexWriter to update is not an option
			 * We can only delete by term, and the index doesn't have a term that
			 * uniquely identify the entry.
			 * But essentially the optimization we are doing is the same Lucene is doing, the only extra cost is the
			 * double file opening.
			 */
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc ) );
		}
		else if ( workType == WorkType.INDEX ) {
			Document doc = getDocument( entity, id );
			queue.add( new DeleteLuceneWork( id, idInString, entityClass ) );
			queue.add( new AddLuceneWork( id, idInString, entityClass, doc, true ) );
		}
		else {
			throw new AssertionFailure( "Unknown WorkType: " + workType );
		}

		super.addWorkToQueue(entityClass, entity, id, workType, queue, searchFactoryImplementor);
	}

	public DirectoryProvider[] getDirectoryProviders() {
		if ( entityState != EntityState.INDEXED ) {
			throw new AssertionFailure( "Contained in only entity: getDirectoryProvider should not have been called." );
		}
		return directoryProviders;
	}

	public IndexShardingStrategy getDirectoryProviderSelectionStrategy() {
		if ( entityState != EntityState.INDEXED ) {
			throw new AssertionFailure(
					"Contained in only entity: getDirectoryProviderSelectionStrategy should not have been called."
			);
		}
		return shardingStrategy;
	}
}
