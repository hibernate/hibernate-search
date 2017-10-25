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

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.inject.scope.HibernateSearchPartitionScoped;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.JobContextUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.jsr352.massindexing.impl.util.PartitionBound;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CACHEABLE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CUSTOM_QUERY_LIMIT;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.FETCH_SIZE;
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
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_SCOPE)
	private String entityManagerFactoryScope;

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
	@BatchProperty(name = MassIndexingJobParameters.CACHEABLE)
	private String serializedCacheable;

	@Inject
	@BatchProperty(name = MassIndexingPartitionProperties.ENTITY_NAME)
	private String entityName;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.FETCH_SIZE)
	private String serializedFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_HQL)
	private String customQueryHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_LIMIT)
	private String serializedCustomQueryLimit;

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

	private EntityManagerFactory emf;

	private Serializable checkpointId;
	private Session session;
	private StatelessSession ss;
	private ScrollableResults scroll;
	private SessionFactory sessionFactory;

	public EntityReader() {
	}

	/**
	 * Constructor for unit test TODO should it be done in this way?
	 */
	EntityReader(String serializedCacheable,
			String entityName,
			String serializedFetchSize,
			String hql,
			String serializedCustomQueryLimit,
			String partitionIdStr,
			String serializedLowerBound,
			String serializedUpperBound) {
		this.serializedCacheable = serializedCacheable;
		this.entityName = entityName;
		this.serializedFetchSize = serializedFetchSize;
		this.customQueryHql = hql;
		this.serializedCustomQueryLimit = serializedCustomQueryLimit;
		this.serializedPartitionId = partitionIdStr;
		this.serializedLowerBound = serializedLowerBound;
		this.serializedUpperBound = serializedUpperBound;
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
			ss.close();
		}
		catch (Exception e) {
			log.unableToCloseStatelessSession( e );
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
	 * @param checkpoint The last checkpoint info persisted in the batch runtime, previously given by checkpointInfo().
	 * If this is the first start, then the checkpoint will be null.
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void open(Serializable checkpointId) throws Exception {
		log.openingReader( serializedPartitionId, entityName );

		final int partitionId = SerializationUtil.parseIntegerParameter( PARTITION_ID, serializedPartitionId );

		JobContextData jobData = getJobContextData();

		PartitionBound bound = getPartitionBound( jobData );
		log.printBound( bound );

		emf = jobData.getEntityManagerFactory();
		ss = PersistenceUtil.openStatelessSession( emf, tenantId );
		session = PersistenceUtil.openSession( emf, tenantId );
		sessionFactory = emf.unwrap( SessionFactory.class );

		PartitionContextData partitionData = null;
		// HQL approach
		// In this approach, the checkpoint mechanism is disabled, because we
		// don't know if the selection is ordered by ID ascendingly in the query.
		if ( customQueryHql != null && !customQueryHql.isEmpty() ) {
			// TODO should I worry about the Lucene AddWork? If this is a
			// restart, will it create duplicate index for the same entity,
			// since there's no purge?
			scroll = buildScrollUsingHQL( ss, customQueryHql );
			partitionData = new PartitionContextData( partitionId, entityName );
		}
		// Criteria approach
		else {
			scroll = buildScrollUsingCriteria( ss, bound, checkpointId, jobData );
			if ( checkpointId == null ) {
				partitionData = new PartitionContextData( partitionId, entityName );
			}
			else {
				partitionData = (PartitionContextData) stepContext.getPersistentUserData();
			}
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
				emfRegistry, entityManagerFactoryScope, entityManagerFactoryReference,
				serializedEntityTypes, serializedCustomQueryCriteria );
	}

	private PartitionBound getPartitionBound(JobContextData jobContextData) throws IOException, ClassNotFoundException {
		Class<?> entityType = jobContextData.getIndexedType( entityName );
		Object lowerBound = SerializationUtil.deserialize( serializedLowerBound );
		Object upperBound = SerializationUtil.deserialize( serializedUpperBound );
		return new PartitionBound( entityType, lowerBound, upperBound );
	}

	private ScrollableResults buildScrollUsingHQL(StatelessSession ss, String HQL) {
		boolean cacheable = SerializationUtil.parseBooleanParameter( CACHEABLE, serializedCacheable );
		int fetchSize = SerializationUtil.parseIntegerParameter( FETCH_SIZE, serializedFetchSize );
		int maxResults = SerializationUtil.parseIntegerParameter( CUSTOM_QUERY_LIMIT, serializedCustomQueryLimit );
		return ss.createQuery( HQL )
				.setReadOnly( true )
				.setCacheable( cacheable )
				.setFetchSize( fetchSize )
				.setMaxResults( maxResults )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	private ScrollableResults buildScrollUsingCriteria(StatelessSession ss,
			PartitionBound unit, Object checkpointId, JobContextData jobData) {
		boolean cacheable = SerializationUtil.parseBooleanParameter( CACHEABLE, serializedCacheable );
		int fetchSize = SerializationUtil.parseIntegerParameter( FETCH_SIZE, serializedFetchSize );
		int maxResults = SerializationUtil.parseIntegerParameter( CUSTOM_QUERY_LIMIT, serializedCustomQueryLimit );
		Class<?> entityType = unit.getEntityType();
		String idName = sessionFactory.getClassMetadata( entityType )
				.getIdentifierPropertyName();

		Criteria criteria = ss.createCriteria( entityType );

		// build criteria using checkpoint ID
		if ( checkpointId != null ) {
			criteria.add( Restrictions.ge( idName, checkpointId ) );
		}

		// build criteria using partition unit
		if ( unit.isUniquePartition() ) {
			// no bounds if the partition unit is unique
		}
		else if ( unit.isFirstPartition() ) {
			criteria.add( Restrictions.lt( idName, unit.getUpperBound() ) );
		}
		else if ( unit.isLastPartition() ) {
			criteria.add( Restrictions.ge( idName, unit.getLowerBound() ) );
		}
		else {
			criteria.add( Restrictions.ge( idName, unit.getLowerBound() ) )
					.add( Restrictions.lt( idName, unit.getUpperBound() ) );
		}

		// build criteria using job context data
		jobData.getCustomQueryCriteria().forEach( c -> criteria.add( c ) );

		return criteria.addOrder( Order.asc( idName ) )
				.setReadOnly( true )
				.setCacheable( cacheable )
				.setFetchSize( fetchSize )
				.setMaxResults( maxResults )
				.scroll( ScrollMode.FORWARD_ONLY );
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
