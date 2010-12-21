/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.util.HibernateHelper;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Represents the set of changes going to be applied to the index for the entities. A stream of Work is feed as input, a
 * list of LuceneWork is output, and in the process we try to reduce the number of output operations to the minimum
 * needed to reach the same final state.
 * 
 * @since 3.3
 * @author Sanne Grinovero
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkPlan {
	
	private static final Logger log = LoggerFactory.make();
	
	private final HashMap<Class<?>, PerClassWork<?>> byClass = new HashMap<Class<?>, PerClassWork<?>>();
	
	private final SearchFactoryImplementor searchFactoryImplementor;
	
	public WorkPlan(SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
	}
	
	/**
	 * most work is split in two, some other might cancel one or more existing works,
	 * we don't track the number accurately as that's not needed.
	 */
	private int approximateWorkQueueSize = 0;
	
	/**
	 * Adds a work to be performed as part of the final plan.
	 * @param <T> the type of the work, or of the affected entity
	 * @param work
	 */
	public <T> void addWork(Work<T> work) {
		approximateWorkQueueSize++;
		Class<T> entityClass = HibernateHelper.getClassFromWork( work );
		PerClassWork classWork = getClassWork( entityClass );
		classWork.addWork( work );
	}
	
	/**
	 * Removes all scheduled work
	 */
	public void clear() {
		byClass.clear();
		approximateWorkQueueSize = 0;
	}
	
	/**
	 * Returns an approximation of the amount of work that
	 * {@link #getPlannedLuceneWork(SearchFactoryImplementor)} would return.
	 * This is meant for resource control for auto flushing of large pending batches.
	 * 
	 * @see Environment#WORKER_BATCHSIZE
	 * @return the approximation
	 */
	public int size() {
		return approximateWorkQueueSize;
	}
	
	/**
	 * Retrieves (and creates if needed) the PerClassWork from the byClass map.
	 */
	private <T> PerClassWork getClassWork(Class<T> entityClass) {
		PerClassWork classWork = byClass.get( entityClass );
		if ( classWork == null ) {
			classWork = new PerClassWork( entityClass );
			byClass.put( entityClass, classWork );
		}
		return classWork;
	}
	
	/**
	 * Makes sure that all additional work needed because of containedIn
	 * is added to the work plan. 
	 */
	public void processContainedInAndPrepareExecution() {
		PerClassWork[] worksFromEvents = new PerClassWork[byClass.size()];
		worksFromEvents = byClass.values().toArray( worksFromEvents );
		// We need to iterate on a "frozen snapshot" of the byClass values
		// because of HSEARCH-647. This method is not recursive, invoked
		// only after the current unit of work is complete, and all additional
		// work we add through recursion is already complete, so we don't need
		// to process again new classes we add during the process.
		for ( PerClassWork perClassWork : worksFromEvents ) {
			perClassWork.processContainedInAndPrepareExecution();
		}
	}
	
	/**
	 * Used for recursive processing of containedIn
	 * @param value the entity to be processed
	 */
	<T> void recurseContainedIn(T value) {
		Class<T> entityClass = HibernateHelper.getClass( value );
		PerClassWork classWork = getClassWork( entityClass );
		classWork.recurseContainedIn( value );
	}
	
	/**
	 * Converts the current plan into a list of LuceneWork,
	 * something that the backends can actually perform to execute
	 * the plan.
	 * @return
	 */
	public List<LuceneWork> getPlannedLuceneWork() {
		List<LuceneWork> luceneQueue = new ArrayList<LuceneWork>();
		for ( PerClassWork perClassWork : byClass.values() ) {
			perClassWork.enqueueLuceneWork( luceneQueue );
		}
		return luceneQueue;
	}
	
	/**
	 * the workPlan organizes work per entity types
	 */
	class PerClassWork<T> {
		
		/**
		 * The type of entities being managed by this instance.
		 * @param clazz
		 */
		PerClassWork(Class<T> clazz) {
			this.entityClass = clazz;
			this.documentBuilder = getEntityBuilder( searchFactoryImplementor, clazz );
		}
		
		/**
		 * We further organize work per entity identifier so that we can cancel or adapt work being done
		 * on the same entities.
		 * This map uses as key what we originally received as {@link Work#getId()} if the type
		 * is annotated with @ProvidedId, otherwise it uses the value pointed to by
		 * {@link org.hibernate.search.annotations.DocumentId} or as last attempt {@link javax.persistence.Id}.
		 */
		private final HashMap<Serializable, PerEntityWork<T>> entityById = new HashMap<Serializable, PerEntityWork<T>>();
		
		/**
		 * When a PurgeAll operation is send on the type, we can remove all previously scheduled work
		 * and remember that the first operation on the index is going to be a purge all.
		 */
		private boolean purgeAll = false;
		
		/**
		 * The type of all classes being managed
		 */
		private final Class<T> entityClass;
		
		/**
		 * The ducomentBuilder relative to the type being managed
		 */
		private final AbstractDocumentBuilder<T> documentBuilder;
		
		/**
		 * Adds a work to the current plan. The entityClass of the work must be of the
		 * type managed by this.
		 * @param work
		 */
		public void addWork(Work<T> work) {
			if ( work.getType() == WorkType.PURGE_ALL ) {
				entityById.clear();
				purgeAll = true;
			}
			else {
				Serializable id = extractProperId( work );
				PerEntityWork<T> entityWork = entityById.get( id );
				if ( entityWork == null ) {
					entityWork = new PerEntityWork<T>( work );
					entityById.put( id, entityWork );
				}
				entityWork.addWork( work );
			}
		}
		
		/**
		 * We need to make a difference on which value is used as identifier
		 * according to use case and mapping options
		 * @param work
		 * @return the appropriate id to use for this work
		 */
		private Serializable extractProperId(Work<T> work) {
			T entity = work.getEntity();
			// 1) entity is null for purge operation, which requires to trust the work id
			// 2) types mapped as provided id require to use the work id
			// 3) when Hibernate identifier rollback is used && this identifier is our same id source, we need to get the value from work id
			if ( entity == null ||
					documentBuilder.requiresProvidedId() ||
					( work.isIdentifierWasRolledBack() && documentBuilder.isIdMatchingJpaId() )
					) {
				return work.getId();
			}
			else {
				return documentBuilder.getId( entity );
			}
		}

		/**
		 * Enqueues all work needed to be performed according to current state into
		 * the LuceneWork queue.
		 * @param luceneQueue work will be appended to this list
		 */
		public void enqueueLuceneWork(List<LuceneWork> luceneQueue) {
			final Set<Entry<Serializable, PerEntityWork<T>>> entityInstances = entityById.entrySet();
			if ( purgeAll ) {
				luceneQueue.add( new PurgeAllLuceneWork( entityClass ) );
			}
			for ( Entry<Serializable, PerEntityWork<T>> entry : entityInstances ) {
				Serializable indexingId = entry.getKey();
				PerEntityWork<T> perEntityWork = entry.getValue();
				perEntityWork.enqueueLuceneWork( entityClass, indexingId, documentBuilder, luceneQueue );
			}
		}
		
		/**
		 * Starts processing the ContainedIn annotation for all instances stored in
		 * byEntityId. Must be performed when no more work is being collected by the event
		 * system, though this same process might recursively add more work to the plan.
		 * Also we switch from a map being keyed by the provided Work Id to a map using as
		 * key the id we will use as DocumentId (which is the same in case the entity is
		 * using @ProvidedId, or otherwise what is marked as @DocumentId or if this is missing
		 * whatever is marked as @Id.
		 */
		public void processContainedInAndPrepareExecution() {
			Entry<String, PerEntityWork<T>>[] entityInstancesFrozenView = new Entry[entityById.size()];
			entityInstancesFrozenView = entityById.entrySet().toArray( entityInstancesFrozenView );
			for ( Entry<String, PerEntityWork<T>> entry : entityInstancesFrozenView ) {
				PerEntityWork<T> perEntityWork = entry.getValue();
				perEntityWork.processContainedIn( documentBuilder, WorkPlan.this );
			}
		}
		
		/**
		 * Method to continue the recursion for ContainedIn processing, as started by {@link #processContainedInAndPrepareExecution()}
		 * Additional work that needs to be processed will be added to this same WorkPlan.
		 * @param value the instance to be processed
		 */
		void recurseContainedIn(T value) {
			if ( documentBuilder.requiresProvidedId() ) {
				log.warn( "@ContainedIn is pointing to an entity having @ProvidedId. This is not supported, indexing of contained in entities will be skipped."
						+ "indexed data of the embedded object might become out of date in objects of type " + HibernateHelper.getClass( value ) );
			}
			else {
				Serializable extractedId = documentBuilder.getId( value );
				if ( extractedId != null ) {
					PerEntityWork<T> entityWork = entityById.get( extractedId );
					if ( entityWork == null ) {
						entityWork = new PerEntityWork( value );
						entityById.put( extractedId, entityWork );
						// recursion starts
						documentBuilder.appendContainedInWorkForInstance( value, WorkPlan.this );
					}
					// else nothing to do as it's being processed already
				}
				else {
					// this branch for @ContainedIn recursive work of non-indexed entities
					// as they don't have an indexingId
					documentBuilder.appendContainedInWorkForInstance( value, WorkPlan.this );
				}
			}
		}
		
	}
	
	/**
	 * Keeps track of what needs to be done Lucene wise for each entity.
	 * Each entity might need to be deleted from the index, added to the index,
	 * or both; in this case delete will be performed first.
	 */
	private static class PerEntityWork<T> {
		
		private T entity;
		
		/**
		 * When true, the Lucene Document representing this entity will be deleted
		 * from the index.
		 */
		private boolean delete = false;
		
		/**
		 * When true, the entity will be converted to a Lucene Document and added
		 * to the index.
		 */
		private boolean add = false;
		
		/**
		 * Any work of type {@link WorkType#INDEX} triggers batch=true for the whole unit of work,
		 * so if any work enables it it stays enabled for everyone.
		 * Having batch=true currently only affects the IndexWriter performance tuning options.
		 */
		private boolean batch = false;
		
		/**
		 * Needed to stop recursion for processing ContainedIn
		 * of already processed instances.
		 */
		private boolean containedInProcessed = false;
		
		/**
		 * Constructor to force an update of the entity even without
		 * having a specific Work instance for it.
		 * @param entity the instance which needs to be updated in the index
		 */
		private PerEntityWork(T entity) {
			// for updates only
			this.entity = entity;
			this.delete = true;
			this.add = true;
			this.containedInProcessed = true;
		}
		
		/**
		 * Prepares the initial state of planned changes according
		 * to the type of work being fired.
		 * @param work
		 */
		private PerEntityWork(Work<T> work) {
			entity = work.getEntity();
			WorkType type = work.getType();
			// sets the initial state:
			switch ( type ) {
			case ADD:
				add = true;
				break;
			case DELETE:
			case PURGE:
				delete = true;
				break;
			case COLLECTION:
			case UPDATE:
				delete = true;
				add = true;
				break;
			case INDEX:
				add = true;
				delete = true;
				batch = true;
				break;
			case PURGE_ALL:
				// not breaking intentionally: PURGE_ALL should not reach this
				// class
			default:
				throw new SearchException( "unexpected state:" + type );
			}
		}
		
		/**
		 * Has different effects depending on the new type of work needed
		 * and the previous scheduled work.
		 * This way we never store more than a plan for each entity and order
		 * of final execution is irrelevant, what matters is the order in which the
		 * work is added to the plan.
		 * @param work
		 */
		public void addWork(Work<T> work) {
			entity = work.getEntity();
			WorkType type = work.getType();
			switch ( type ) {
			case INDEX:
				batch = true;
				// not breaking intentionally
			case UPDATE:
				if ( add && !delete ) {
					// noop: the entity was newly created in this same unit of work
					// so it needs to be added no need to delete
				}
				else {
					add = true;
					delete = true;
				}
				break;
			case ADD: // Is the only operation which doesn't imply a delete-before-add
				add = true;
				// leave delete flag as-is
				break;
			case DELETE:
			case PURGE:
				if ( add && !delete ) {
					// the entity was was newly created in this same unit of
					// work so works counter each other
					add = false;
				}
				else {
					add = false;
					delete = true;
				}
				break;
			case COLLECTION:
				if ( !add && !delete ) {
					add = true;
					delete = true;
				}
				// nothing to do, as something else was done
				break;
			case PURGE_ALL:
			default:
				throw new SearchException( "unexpected state:" + type );
			}
		}
		
		/**
		 * Adds the needed LuceneWork to the queue for this entity instance
		 * @param entityClass the type
		 * @param indexingId identifier of the instance
		 * @param entityBuilder the DocumentBuilder for this type
		 * @param luceneQueue the queue collecting all changes
		 */
		public void enqueueLuceneWork(Class<T> entityClass, Serializable indexingId, AbstractDocumentBuilder<T> entityBuilder, List<LuceneWork> luceneQueue) {
			if ( add || delete ) {
				entityBuilder.addWorkToQueue( entityClass, entity, indexingId, delete, add, batch, luceneQueue );
			}
		}
		
		/**
		 * Works via recursion passing the WorkPlan over, so that additional work can be planned
		 * according to the needs of ContainedIn processing.
		 * @see org.hibernate.search.annotations.ContainedIn
		 * @param entityBuilder the DocumentBuilder for this type
		 * @param workplan the current WorkPlan, used for recursion
		 */
		public void processContainedIn(AbstractDocumentBuilder<T> entityBuilder, WorkPlan workplan) {
			if ( ! containedInProcessed ) {
				containedInProcessed = true;
				if ( add || delete ) {
					entityBuilder.appendContainedInWorkForInstance( entity, workplan );
				}
			}
		}
	}
	
	/**
	 * Get and cache the DocumentBuilder for this type. Being this a perClassWork
	 * we can fetch it once.
	 * @return the DocumentBuilder for this type
	 */
	private static <T> AbstractDocumentBuilder<T> getEntityBuilder(SearchFactoryImplementor searchFactoryImplementor, Class entityClass) {
		AbstractDocumentBuilder<T> entityBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityClass );
		if ( entityBuilder == null ) {
			entityBuilder = searchFactoryImplementor.getDocumentBuilderContainedEntity( entityClass );
			if ( entityBuilder == null ) {
				// should never happen but better be safe than sorry
				throw new SearchException( "Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityClass );
			}
		}
		return entityBuilder;
	}

}

