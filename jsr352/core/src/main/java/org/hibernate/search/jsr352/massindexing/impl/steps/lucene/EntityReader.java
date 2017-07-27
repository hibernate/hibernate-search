/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.IOException;
import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.inject.scope.HibernateSearchPartitionScoped;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.EntityTypeDescriptor;
import org.hibernate.search.jsr352.massindexing.impl.util.IdOrder;
import org.hibernate.search.jsr352.massindexing.impl.util.JobContextUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.jsr352.massindexing.impl.util.PartitionBound;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CACHE_MODE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CHECKPOINT_INTERVAL;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ENTITY_FETCH_SIZE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY;
import static org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties.PARTITION_ID;

/**
 * Entity reader reads entities from database. During the open of the read stream, this reader builds a scrollable
 * result. Then, it scrolls from one entity to another at each reading. An entity reader reaches its end when there’s no
 * more item to read. Each reader contains only one entity type.
 * <p>
 * The reading range is restricted by the {@link PartitionBound}, which always represents as a left-closed interval.
 * See {@link PartitionMapper} for more information about these bounds.
 *
 * @author Mincong Huang
 */
// Same hack as in JobContextSetupListener.
@Named(value = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader")
@HibernateSearchPartitionScoped
public class EntityReader extends AbstractItemReader {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE)
	private String entityManagerFactoryNamespace;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_TYPES)
	private String serializedEntityTypes;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA)
	private String serializedCustomQueryCriteria;

	@Inject
	private EntityManagerFactoryRegistry emfRegistry;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CACHE_MODE)
	private String serializedCacheMode;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_FETCH_SIZE)
	private String serializedEntityFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_HQL)
	private String customQueryHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY)
	private String serializedMaxResultsPerEntity;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.PARTITION_ID)
	private String serializedPartitionId;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.LOWER_BOUND)
	private String serializedLowerBound;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.UPPER_BOUND)
	private String serializedUpperBound;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.INDEX_SCOPE)
	private String indexScopeName;

	private EntityManagerFactory emf;

	private Serializable checkpointId;
	private Session session;
	private ScrollableResults scroll;

	public EntityReader() {
	}

	/**
	 * Constructor for unit test TODO should it be done in this way?
	 */
	EntityReader(String serializedCacheMode,
			String entityName,
			String serializedEntityFetchSize,
			String serializedCheckpointInterval,
			String hql,
			String serializedMaxResultsPerEntity,
			String partitionIdStr,
			String serializedLowerBound,
			String serializedUpperBound,
			String indexScopeName) {
		this.serializedCacheMode = serializedCacheMode;
		this.entityName = entityName;
		this.serializedEntityFetchSize = serializedEntityFetchSize;
		this.serializedCheckpointInterval = serializedCheckpointInterval;
		this.customQueryHql = hql;
		this.serializedMaxResultsPerEntity = serializedMaxResultsPerEntity;
		this.serializedPartitionId = partitionIdStr;
		this.serializedLowerBound = serializedLowerBound;
		this.serializedUpperBound = serializedUpperBound;
		this.indexScopeName = indexScopeName;
	}

	/**
	 * The checkpointInfo method returns the current checkpoint data for this reader. It is called before a chunk
	 * checkpoint is committed.
	 *
	 * @return the checkpoint info
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception {
		log.checkpointReached( entityName, checkpointId );
		return checkpointId;
	}

	/**
	 * Close operation(s) before the class destruction.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		log.closingReader( serializedPartitionId, entityName );
		try {
			scroll.close();
		}
		catch (Exception e) {
			log.unableToCloseScrollableResults( e );
		}
		try {
			session.close();
		}
		catch (Exception e) {
			log.unableToCloseSession( e );
		}
		// reset the chunk work count to avoid over-count in item collector
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		stepContext.setPersistentUserData( partitionData );
	}

	/**
	 * Initialize the environment. If checkpoint does not exist, then it should be the first open. If checkpoint exists,
	 * then it isn't the first open, re-use the input object "checkpoint" as the last ID already read.
	 *
	 * @param checkpointId The last checkpoint info persisted in the batch runtime, previously given by checkpointInfo().
	 * If this is the first start, then the checkpoint will be null.
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void open(Serializable checkpointId) throws Exception {
		log.openingReader( serializedPartitionId, entityName );

		final int partitionId = SerializationUtil.parseIntegerParameter( PARTITION_ID, serializedPartitionId );
		boolean isRestarted = checkpointId != null;
		JobContextData jobData = getJobContextData();

		PartitionBound bound = getPartitionBound( jobData );
		if ( isRestarted ) {
			bound.setLowerBound( checkpointId );
		}
		log.printBound( bound );

		emf = jobData.getEntityManagerFactory();
		session = PersistenceUtil.openSession( emf, tenantId );

		PartitionContextData partitionData;
		IndexScope indexScope = IndexScope.valueOf( indexScopeName );
		switch ( indexScope ) {
			case HQL:
				scroll = buildScrollUsingHQL( session, customQueryHql );
				partitionData = new PartitionContextData( partitionId, entityName, indexScope );
				break;

			case CRITERIA:
			case FULL_ENTITY:
				scroll = buildScrollUsingCriteria( session, bound, jobData );
				if ( isRestarted ) {
					partitionData = (PartitionContextData) stepContext.getPersistentUserData();
				}
				else {
					partitionData = new PartitionContextData( partitionId, entityName, indexScope );
				}
				break;

			default:
				// This should never happen.
				throw new IllegalStateException( "Unknown value from enum: " + IndexScope.class );
		}
		stepContext.setTransientUserData( partitionData );
	}

	/*
	 * The spec states that a new job context is created for each partition,
	 * meaning that the entity reader may not be able to re-use the job context
	 * data set up in the JobContextSetupListener (actually we can, but only with JBeret).
	 * Thus we take care to re-create the data if necessary.
	 *
	 * See https://github.com/WASdev/standards.jsr352.jbatch/issues/50
	 */
	private JobContextData getJobContextData() throws ClassNotFoundException, IOException {
		return JobContextUtil.getOrCreateData( jobContext,
				emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference,
				serializedEntityTypes, serializedCustomQueryCriteria );
	}

	private PartitionBound getPartitionBound(JobContextData jobContextData) throws IOException, ClassNotFoundException {
		Class<?> entityType = jobContextData.getEntityType( entityName );
		Object lowerBound = SerializationUtil.deserialize( serializedLowerBound );
		Object upperBound = SerializationUtil.deserialize( serializedUpperBound );
		IndexScope indexScope = IndexScope.valueOf( indexScopeName );
		return new PartitionBound( entityType, lowerBound, upperBound, indexScope );
	}

	private ScrollableResults buildScrollUsingHQL(Session session, String HQL) {
		Query query = session.createQuery( HQL );

		CacheMode cacheMode = SerializationUtil.parseCacheModeParameter( CACHE_MODE, serializedCacheMode );

		int entityFetchSize;
		if ( StringHelper.isNotEmpty( serializedEntityFetchSize ) ) {
			entityFetchSize = SerializationUtil.parseIntegerParameter( ENTITY_FETCH_SIZE, serializedEntityFetchSize );
		}
		else {
			entityFetchSize = SerializationUtil.parseIntegerParameter( CHECKPOINT_INTERVAL, serializedCheckpointInterval );
		}

		if ( StringHelper.isNotEmpty( serializedMaxResultsPerEntity ) ) {
			int maxResults = SerializationUtil.parseIntegerParameter( MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity );
			query.setMaxResults( maxResults );
		}
		return query.setReadOnly( true )
				.setCacheMode( cacheMode )
				.setFetchSize( entityFetchSize )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	private ScrollableResults buildScrollUsingCriteria(Session session,
			PartitionBound bound, JobContextData jobData) throws Exception {
		Class<?> entity = bound.getEntityType();
		EntityTypeDescriptor typeDescriptor = jobData.getEntityTypeDescriptor( entity );
		IdOrder idOrder = typeDescriptor.getIdOrder();
		Criteria criteria = session.createCriteria( entity );

		// build orders for this entity
		idOrder.addAscOrder( criteria );

		// build criteria using partition unit
		applyBound( criteria, idOrder, bound );

		// build criteria using job context data
		jobData.getCustomQueryCriteria().forEach( c -> criteria.add( c ) );

		CacheMode cacheMode = SerializationUtil.parseCacheModeParameter( CACHE_MODE, serializedCacheMode );

		if ( StringHelper.isNotEmpty( serializedMaxResultsPerEntity ) ) {
			int maxResults = SerializationUtil.parseIntegerParameter( MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity );
			criteria.setMaxResults( maxResults );
		}

		int entityFetchSize;
		if ( StringHelper.isNotEmpty( serializedEntityFetchSize ) ) {
			entityFetchSize = SerializationUtil.parseIntegerParameter( ENTITY_FETCH_SIZE, serializedEntityFetchSize );
		}
		else {
			entityFetchSize = SerializationUtil.parseIntegerParameter( CHECKPOINT_INTERVAL, serializedCheckpointInterval );
		}

		return criteria.setReadOnly( true )
				.setCacheMode( cacheMode )
				.setFetchSize( entityFetchSize )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	private static void applyBound(Criteria criteria, IdOrder idOrder, PartitionBound partitionBound) {
		if ( partitionBound.hasUpperBound() ) {
			Object upperBound = partitionBound.getUpperBound();
			criteria.add( idOrder.idLesser( upperBound ) );
		}
		if ( partitionBound.hasLowerBound() ) {
			Object lowerBound = partitionBound.getLowerBound();
			criteria.add( idOrder.idGreaterOrEqual( lowerBound ) );
		}
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one entity fetched.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Object readItem() throws Exception {
		log.readingEntity();
		Object entity = null;

		if ( scroll.next() ) {
			entity = scroll.get( 0 );
			checkpointId = (Serializable) emf.getPersistenceUnitUtil()
					.getIdentifier( entity );
		}
		else {
			log.noMoreResults();
		}
		return entity;
	}
}
