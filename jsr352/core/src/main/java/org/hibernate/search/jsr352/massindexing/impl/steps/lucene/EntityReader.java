/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.PartitionBound;
import org.jboss.logging.Logger;

/**
 * Item reader reads entities using scrollable results. For each reader, there's only one target entity type. The range
 * to read is defined by the partition unit. This range is always a left-closed interval.
 * <p>
 * For example, there 2 entity types Company and Employee. The number of rows are respectively 5 and 4500. The
 * rowsPerPartition is set to 1000. Then, there will be 6 readers and their ranges are :
 * <ul>
 * <li>partitionId = 0, entityType = Company, range = [null, null[
 * <li>partitionId = 1, entityType = Employee, range = [null, 1000[
 * <li>partitionId = 2, entityType = Employee, range = [1000, 2000[
 * <li>partitionId = 3, entityType = Employee, range = [2000, 3000[
 * <li>partitionId = 4, entityType = Employee, range = [3000, 4000[
 * <li>partitionId = 5, entityType = Employee, range = [4000, null[
 * </ul>
 *
 * @author Mincong Huang
 */
public class EntityReader extends AbstractItemReader {

	private static final Logger LOGGER = Logger.getLogger( EntityReader.class );

	@Inject
	private JobContext jobContext;

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty
	private String cacheable;

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	@BatchProperty
	private String fetchSize;

	@Inject
	@BatchProperty
	private String hql;

	@Inject
	@BatchProperty
	private String maxResults;

	@Inject
	@BatchProperty(name = "partitionId")
	private String partitionIdStr;

	private EntityManagerFactory emf;

	private Class<?> entityType;
	private Serializable checkpointId;
	private Session session;
	private StatelessSession ss;
	private ScrollableResults scroll;
	private SessionFactory sessionFactory;

	public EntityReader() {
	}

	/**
	 * Constructor for unit test TODO should it be done in this way?
	 *
	 * @param cacheable
	 * @param entityName
	 * @param fetchSize
	 * @param hql
	 * @param maxResults
	 * @param partitionIdStr
	 */
	EntityReader(String cacheable,
			String entityName,
			String fetchSize,
			String hql,
			String maxResults,
			String partitionIdStr) {
		this.cacheable = cacheable;
		this.entityName = entityName;
		this.fetchSize = fetchSize;
		this.hql = hql;
		this.maxResults = maxResults;
		this.partitionIdStr = partitionIdStr;
	}

	/**
	 * The checkpointInfo method returns the current checkpoint data for this reader. It is called before a chunk
	 * checkpoint is committed.
	 *
	 * @return the checkpoint info
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception {
		LOGGER.debug( "checkpointInfo() called. "
				+ "Saving last read ID to batch runtime..." );
		return checkpointId;
	}

	/**
	 * Close operation(s) before the class destruction.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		LOGGER.debug( "closing everything..." );
		try {
			scroll.close();
			LOGGER.debug( "Scrollable results closed." );
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
		try {
			ss.close();
			LOGGER.debug( "Stateless session closed." );
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
		try {
			session.close();
			LOGGER.debug( "Session closed." );
		}
		catch (Exception e) {
			LOGGER.error( e );
		}
		// reset the chunk work count to avoid over-count in item collector
		// release session
		PartitionContextData partitionData = (PartitionContextData) stepContext.getTransientUserData();
		partitionData.setSession( null );
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

		final int partitionId = Integer.parseInt( partitionIdStr );

		LOGGER.debugf( "[partitionId=%d] open reader for entity %s ...", (Integer) partitionId, entityName );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		entityType = jobData.getIndexedType( entityName );
		PartitionBound bound = jobData.getPartitionBound( partitionId );
		LOGGER.debug( bound );

		emf = jobData.getEntityManagerFactory();
		sessionFactory = emf.unwrap( SessionFactory.class );
		ss = sessionFactory.openStatelessSession();
		session = sessionFactory.openSession();

		PartitionContextData partitionData = null;
		// HQL approach
		// In this approach, the checkpoint mechanism is disabled, because we
		// don't know if the selection is ordered by ID ascendingly in the query.
		if ( hql != null && !hql.isEmpty() ) {
			// TODO should I worry about the Lucene AddWork? If this is a
			// restart, will it create duplicate index for the same entity,
			// since there's no purge?
			scroll = buildScrollUsingHQL( ss, hql );
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

		partitionData.setSession( session );
		stepContext.setTransientUserData( partitionData );
	}

	private ScrollableResults buildScrollUsingHQL(StatelessSession ss, String HQL) {
		return ss.createQuery( HQL )
				.setReadOnly( true )
				.setCacheable( Boolean.parseBoolean( cacheable ) )
				.setFetchSize( Integer.parseInt( fetchSize ) )
				.setMaxResults( Integer.parseInt( maxResults ) )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	private ScrollableResults buildScrollUsingCriteria(StatelessSession ss,
			PartitionBound unit, Object checkpointId, JobContextData jobData) {
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
		jobData.getCriteria().forEach( c -> criteria.add( c ) );

		return criteria.addOrder( Order.asc( idName ) )
				.setReadOnly( true )
				.setCacheable( Boolean.parseBoolean( cacheable ) )
				.setFetchSize( Integer.parseInt( fetchSize ) )
				.setMaxResults( Integer.parseInt( maxResults ) )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one entity fetched.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Object readItem() throws Exception {
		LOGGER.debug( "Reading item ..." );
		Object entity = null;

		if ( scroll.next() ) {
			entity = scroll.get( 0 );
			checkpointId = (Serializable) emf.getPersistenceUnitUtil()
					.getIdentifier( entity );
		}
		else {
			LOGGER.debug( "no more result. read ends." );
		}
		return entity;
	}
}
