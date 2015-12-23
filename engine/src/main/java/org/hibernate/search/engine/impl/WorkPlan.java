/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.ContainedInRecursionContext;
import org.hibernate.search.engine.spi.DocumentBuilderContainedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Represents the set of changes going to be applied to the index for the entities. A stream of Work is feed as input, a
 * list of LuceneWork is output, and in the process we try to reduce the number of output operations to the minimum
 * needed to reach the same final state.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Martin Braun
 * @since 3.3
 */
@SuppressWarnings( { "rawtypes", "unchecked" })
public class WorkPlan {

	private static final Log log = LoggerFactory.make();

	private final HashMap<Class<?>, PerClassWork> byClass = new HashMap<Class<?>, PerClassWork>();

	private final ExtendedSearchIntegrator extendedIntegrator;

	private final InstanceInitializer instanceInitializer;

	/**
	 * most work is split in two, some other might cancel one or more existing works,
	 * we don't track the number accurately as that's not needed.
	 */
	private int approximateWorkQueueSize = 0;

	public WorkPlan(ExtendedSearchIntegrator extendedIntegrator) {
		this.extendedIntegrator = extendedIntegrator;
		this.instanceInitializer = extendedIntegrator.getInstanceInitializer();
	}

	/**
	 * Adds a work to be performed as part of the final plan.
	 *
	 * @param work The work instance to add to the work plan
	 */
	public void addWork(Work work) {
		approximateWorkQueueSize++;
		Class<?> entityClass = instanceInitializer.getClassFromWork( work );
		PerClassWork classWork = getClassWork( work.getTenantIdentifier(), entityClass );
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
	 * Returns an approximation of the amount of work in the queue.
	 * This is meant for resource control for auto flushing of large pending batches.
	 *
	 * @return the approximation
	 * @see org.hibernate.search.cfg.Environment#QUEUEINGPROCESSOR_BATCHSIZE
	 */
	public int size() {
		return approximateWorkQueueSize;
	}

	/**
	 * @param tenantId the tenant identifier
	 * @param entityClass The entity class for which to retrieve the work
	 *
	 * @return returns (and creates if needed) the {@code PerClassWork} from the {@link #byClass} map.
	 */
	private PerClassWork getClassWork(String tenantId, Class<?> entityClass) {
		PerClassWork classWork = byClass.get( entityClass );
		if ( classWork == null ) {
			classWork = new PerClassWork( tenantId, entityClass );
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
	 *
	 * @param <T> the type of the entity
	 * @param value the entity to be processed
	 * @param context the validator for the depth constraints
	 * @param tenantId the tenant identifier. It can be null.
	 */
	public <T> void recurseContainedIn(T value, ContainedInRecursionContext context, String tenantId) {
		Class<T> entityClass = instanceInitializer.getClass( value );
		PerClassWork classWork = getClassWork( tenantId, entityClass );
		classWork.recurseContainedIn( value, context );
	}

	/**
	 * @return returns the current plan converted as a list of {@code LuceneWork}
	 */
	public List<LuceneWork> getPlannedLuceneWork() {
		List<LuceneWork> luceneQueue = new ArrayList<LuceneWork>();
		for ( PerClassWork perClassWork : byClass.values() ) {
			perClassWork.enqueueLuceneWork( luceneQueue );
		}
		return luceneQueue;
	}

	/**
	 * {@code PerClassWork} organizes work per entity type.
	 */
	class PerClassWork {

		/**
		 * We further organize work per entity identifier so that we can cancel or adapt work being done
		 * on the same entities.
		 * This map uses as key what we originally received as {@link Work#getId()} if the type
		 * is annotated with @ProvidedId, otherwise it uses the value pointed to by
		 * {@link org.hibernate.search.annotations.DocumentId} or as last attempt {@code javax.persistence.Id}.
		 */
		private final Map<Serializable, PerEntityWork> entityById = new HashMap<Serializable, PerEntityWork>();

		/**
		 * When a PurgeAll operation is send on the type, we can remove all previously scheduled work
		 * and remember that the first operation on the index is going to be a purge all.
		 */
		private boolean purgeAll = false;

		private List<DeletionQuery> deletionQueries = new ArrayList<>();

		/**
		 * The type of all classes being managed
		 */
		private final Class<?> entityClass;

		private final String tenantId;

		/**
		 * The DocumentBuilder relative to the type being managed
		 */
		private final AbstractDocumentBuilder documentBuilder;

		/**
		 * The entity {@link #entityClass} does not have its own index, but is only used in contained scenarios
		 */
		private final boolean containedInOnly;

		/**
		 * @param clazz The type of entities being managed by this instance
		 */
		PerClassWork(String tenantId, Class<?> clazz) {
			this.entityClass = clazz;
			this.documentBuilder = getEntityBuilder( extendedIntegrator, clazz );
			this.containedInOnly = documentBuilder instanceof DocumentBuilderContainedEntity;
			this.tenantId = tenantId;
		}

		/**
		 * Adds a work to the current plan. The entityClass of the work must be of the
		 * type managed by this.
		 *
		 * @param work the {@code Work} instance to add to the plan
		 */
		public void addWork(Work work) {
			if ( work.getType() == WorkType.PURGE_ALL ) {
				entityById.clear();
				this.deletionQueries.clear();
				purgeAll = true;
			}
			else if ( work.getType() == WorkType.DELETE_BY_QUERY ) {
				DeleteByQueryWork delWork = (DeleteByQueryWork) work;
				this.deletionQueries.add( delWork.getDeleteByQuery() );
			}
			else {
				Serializable id = extractProperId( work );
				PerEntityWork entityWork = entityById.get( id );
				if ( entityWork == null ) {
					entityWork = new PerEntityWork( work );
					entityById.put( id, entityWork );
				}
				entityWork.addWork( work );
			}
		}

		/**
		 * We need to make a difference on which value is used as identifier
		 * according to use case and mapping options
		 *
		 * @param work The work instance from which to extract the id
		 *
		 * @return the appropriate id to use for this work
		 */
		private Serializable extractProperId(Work work) {
			// see HSEARCH-662
			if ( containedInOnly ) {
				return work.getId();
			}

			Object entity = work.getEntity();
			// 1) entity is null for purge operation, which requires to trust the work id
			// 2) types mapped as provided id require to use the work id
			// 3) when Hibernate identifier rollback is used && this identifier is our same id source, we need to get the value from work id
			if ( entity == null
					|| documentBuilder.requiresProvidedId()
					|| ( work.isIdentifierWasRolledBack() && documentBuilder.isIdMatchingJpaId() ) ) {
				return work.getId();
			}
			else {
				return documentBuilder.getId( entity );
			}
		}

		/**
		 * Enqueues all work needed to be performed according to current state into
		 * the LuceneWork queue.
		 *
		 * @param luceneQueue work will be appended to this list
		 */
		public void enqueueLuceneWork(List<LuceneWork> luceneQueue) {
			final Set<Entry<Serializable, PerEntityWork>> entityInstances = entityById.entrySet();
			ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
			if ( purgeAll ) {
				luceneQueue.add( new PurgeAllLuceneWork( tenantId, entityClass ) );
			}
			for ( DeletionQuery delQuery : this.deletionQueries ) {
				luceneQueue.add( new DeleteByQueryLuceneWork( tenantId, entityClass, delQuery ) );
			}
			for ( Entry<Serializable, PerEntityWork> entry : entityInstances ) {
				Serializable indexingId = entry.getKey();
				PerEntityWork perEntityWork = entry.getValue();
				String tenantIdentifier = perEntityWork.getTenantIdentifier();
				perEntityWork.enqueueLuceneWork( tenantIdentifier, entityClass, indexingId, documentBuilder, luceneQueue, conversionContext );
			}
		}

		/**
		 * Starts processing the {@code ContainedIn} annotation for all instances stored in
		 * {@link #entityById}.
		 *
		 * This processing must be performed when no more work is being collected by the event
		 * system. The processing might recursively add more work to the plan.
		 */
		public void processContainedInAndPrepareExecution() {
			Entry<String, PerEntityWork>[] entityInstancesFrozenView = new Entry[entityById.size()];
			entityInstancesFrozenView = entityById.entrySet().toArray( entityInstancesFrozenView );
			for ( Entry<String, PerEntityWork> entry : entityInstancesFrozenView ) {
				PerEntityWork perEntityWork = entry.getValue();
				perEntityWork.processContainedIn( documentBuilder, WorkPlan.this );
			}
		}

		/**
		 * Method to continue the recursion for ContainedIn processing, as started by {@link #processContainedInAndPrepareExecution()}
		 * Additional work that needs to be processed will be added to this same WorkPlan.
		 *
		 * @param value the instance to be processed
		 */
		void recurseContainedIn(Object value, ContainedInRecursionContext context) {
			if ( documentBuilder.requiresProvidedId() ) {
				log.containedInPointsToProvidedId( instanceInitializer.getClass( value ) );
			}
			else {
				Serializable extractedId = documentBuilder.getId( value );
				if ( extractedId != null ) {
					PerEntityWork entityWork = entityById.get( extractedId );
					if ( entityWork == null ) {
						EntityIndexingInterceptor entityInterceptor = getEntityInterceptor();
						IndexingOverride operation;
						if ( entityInterceptor != null ) {
							operation = entityInterceptor.onUpdate( value );
						}
						else {
							operation = IndexingOverride.APPLY_DEFAULT;
						}
						//TODO there is a small duplication with some of TransactionalWorker.interceptWork
						//     but what would be a proper factored solution?
						switch ( operation ) {
							//we are planning an update by default
							case UPDATE:
							case APPLY_DEFAULT:
								entityWork = new PerEntityWork( tenantId, value );
								entityById.put( extractedId, entityWork );
								break;
							case SKIP:
								log.forceSkipIndexOperationViaInterception( entityClass, WorkType.UPDATE );
								break;
							case REMOVE:
								log.forceRemoveOnIndexOperationViaInterception( entityClass, WorkType.UPDATE );
								Work work = new Work( tenantId, value, extractedId, WorkType.DELETE );
								entityWork = new PerEntityWork( work );
								entityById.put( extractedId, entityWork );
								break;
							default:
								throw new AssertionFailure( "Unknown action type: " + operation );
						}
						// recursion starts
						documentBuilder.appendContainedInWorkForInstance( value, WorkPlan.this, context );
					}
					// else nothing to do as it's being processed already
				}
				else {
					// this branch for @ContainedIn recursive work of non-indexed entities
					// as they don't have an indexingId
					documentBuilder.appendContainedInWorkForInstance( value, WorkPlan.this, context );
				}
			}
		}

		private EntityIndexingInterceptor getEntityInterceptor() {
			EntityIndexBinding indexBindingForEntity = extendedIntegrator.getIndexBinding(
					entityClass
			);
			return indexBindingForEntity != null ? indexBindingForEntity.getEntityIndexingInterceptor() : null;
		}

		public String getTenantId() {
			return tenantId;
		}
	}

	/**
	 * Keeps track of what needs to be done Lucene wise for each entity.
	 * Each entity might need to be deleted from the index, added to the index,
	 * or both; in this case delete will be performed first.
	 */
	private static class PerEntityWork {

		private Object entity;

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
		 * Needed to stop recursion for processing ContainedIn
		 * of already processed instances.
		 */
		private boolean containedInProcessed = false;

		private final String tenantId;

		/**
		 * Constructor to force an update of the entity even without
		 * having a specific Work instance for it.
		 *
		 * @param entity the instance which needs to be updated in the index
		 */
		private PerEntityWork(String tenantId, Object entity) {
			// for updates only
			this.entity = entity;
			this.delete = true;
			this.add = true;
			this.containedInProcessed = true;
			this.tenantId = tenantId;
		}

		/**
		 * Prepares the initial state of planned changes according
		 * to the type of work being fired.
		 *
		 * @param work the work instance
		 */
		private PerEntityWork(Work work) {
			entity = work.getEntity();
			tenantId = work.getTenantIdentifier();
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
					break;
				case PURGE_ALL:
					// not breaking intentionally: PURGE_ALL should not reach this
					// class
				case DELETE_BY_QUERY:
					// not breaking intentionally: DELETE_BY_QUERY should not reach
					// this class
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
		 *
		 * @param work the work instance to add
		 */
		public void addWork(Work work) {
			entity = work.getEntity();
			WorkType type = work.getType();
			switch ( type ) {
				case INDEX:
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
				case DELETE_BY_QUERY:
				default:
					throw new SearchException( "unexpected state:" + type );
			}
		}

		/**
		 * Adds the needed LuceneWork to the queue for this entity instance
		 *
		 * @param tenantIdentifier the tenant identifier
		 * @param entityClass the type
		 * @param indexingId identifier of the instance
		 * @param entityBuilder the DocumentBuilder for this type
		 * @param luceneQueue the queue collecting all changes
		 */
		public void enqueueLuceneWork(String tenantIdentifier, Class entityClass, Serializable indexingId, AbstractDocumentBuilder entityBuilder,
				List<LuceneWork> luceneQueue, ConversionContext conversionContext) {
			if ( add || delete ) {
				entityBuilder.addWorkToQueue( tenantIdentifier, entityClass, entity, indexingId, delete, add, luceneQueue, conversionContext );
			}
		}

		/**
		 * Works via recursion passing the WorkPlan over, so that additional work can be planned
		 * according to the needs of ContainedIn processing.
		 *
		 * @param entityBuilder the DocumentBuilder for this type
		 * @param workplan the current WorkPlan, used for recursion
		 *
		 * @see org.hibernate.search.annotations.ContainedIn
		 */
		public void processContainedIn(AbstractDocumentBuilder entityBuilder, WorkPlan workplan) {
			if ( entity != null && !containedInProcessed ) {
				containedInProcessed = true;
				if ( add || delete ) {
					entityBuilder.appendContainedInWorkForInstance( entity, workplan, null, getTenantIdentifier() );
				}
			}
		}

		public String getTenantIdentifier() {
			return tenantId;
		}
	}

	/**
	 * Get and cache the DocumentBuilder for this type. Being this a perClassWork
	 * we can fetch it once.
	 *
	 * @param extendedIntegrator the search factory (implementor)
	 * @param entityClass the entity type for which to retrieve the document builder
	 *
	 * @return the DocumentBuilder for this type
	 */
	private static AbstractDocumentBuilder getEntityBuilder(ExtendedSearchIntegrator extendedIntegrator, Class<?> entityClass) {
		EntityIndexBinding entityIndexBinding = extendedIntegrator.getIndexBinding( entityClass );
		if ( entityIndexBinding == null ) {
			DocumentBuilderContainedEntity entityBuilder = extendedIntegrator.getDocumentBuilderContainedEntity(
					entityClass
			);
			if ( entityBuilder == null ) {
				// should never happen but better be safe than sorry
				throw new SearchException(
						"Unable to perform work. Entity Class is not @Indexed nor hosts @ContainedIn: " + entityClass
				);
			}
			else {
				return entityBuilder;
			}
		}
		else {
			return entityIndexBinding.getDocumentBuilder();
		}
	}
}
