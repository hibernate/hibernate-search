/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ID_FETCH_SIZE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.MAX_THREADS;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ROWS_PER_PARTITION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.impl.util.EntityTypeDescriptor;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexingPartitionProperties;
import org.hibernate.search.jsr352.massindexing.impl.util.PartitionBound;
import org.hibernate.search.jsr352.massindexing.impl.util.PersistenceUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This partition mapper provides a dynamic partition plan for chunk processing.
 * <p>
 * A partition plan specifies the number of partitions, which is calculated based on the quantity of entities selected,
 * and the value of job parameter {@code MassIndexingJobParameters.ROWS_PER_PARTITION} defined by the user.
 * <p>
 * For example, there are 2 entity types Company and Employee. The number of rows are respectively 5 and 4500.
 * Row identifiers start at 0. The rowsPerPartition is set to 1000.
 * Then, there will be 6 partitions and their ranges will be:
 * <ul>
 * <li>partitionId = 0, entityType = Company, range = [null, null[ (effectively [0, 4])
 * <li>partitionId = 1, entityType = Employee, range = [null, 1000[ (effectively [0, 999])
 * <li>partitionId = 2, entityType = Employee, range = [1000, 2000[ (effectively [1000, 1999])
 * <li>partitionId = 3, entityType = Employee, range = [2000, 3000[ (effectively [2000, 2999])
 * <li>partitionId = 4, entityType = Employee, range = [3000, 4000[ (effectively [3000, 3999])
 * <li>partitionId = 5, entityType = Employee, range = [4000, null[ (effectively [4000, 4999]
 * </ul>
 *
 * @author Mincong Huang
 */
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = ID_FETCH_SIZE)
	private String serializedIdFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_HQL)
	private String customQueryHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_THREADS)
	private String serializedMaxThreads;

	@Inject
	@BatchProperty(name = MAX_RESULTS_PER_ENTITY)
	private String serializedMaxResultsPerEntity;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ROWS_PER_PARTITION)
	private String serializedRowsPerPartition;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private EntityManagerFactory emf;

	public PartitionMapper() {
	}

	/**
	 * Constructor for unit test. TODO should it be done in this way?
	 *
	 * @param emf
	 * @param customQueryHql
	 */
	PartitionMapper(EntityManagerFactory emf,
			String serializedIdFetchSize,
			String customQueryHql,
			String serializedRowsPerPartition,
			String serializedMaxThreads) {
		this.emf = emf;
		this.serializedIdFetchSize = serializedIdFetchSize;
		this.customQueryHql = customQueryHql;
		this.serializedMaxThreads = serializedMaxThreads;
		this.serializedRowsPerPartition = serializedRowsPerPartition;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		Session session = null;
		StatelessSession ss = null;

		try {
			emf = jobData.getEntityManagerFactory();
			session = PersistenceUtil.openSession( emf, tenantId );
			ss = PersistenceUtil.openStatelessSession( emf, tenantId );

			Integer maxResults = null;
			if ( StringHelper.isNotEmpty( serializedMaxResultsPerEntity ) ) {
				maxResults = SerializationUtil.parseIntegerParameter( MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity );
			}
			int rowsPerPartition = SerializationUtil.parseIntegerParameter( ROWS_PER_PARTITION, serializedRowsPerPartition );
			int idFetchSize = SerializationUtil.parseIntegerParameter( ID_FETCH_SIZE, serializedIdFetchSize );

			List<EntityTypeDescriptor> entityTypeDescriptors = jobData.getEntityTypeDescriptors();
			List<PartitionBound> partitionBounds = new ArrayList<>();

			switch ( PersistenceUtil.getIndexScope( customQueryHql, jobData.getCustomQueryCriteria() ) ) {
				case HQL:
					Class<?> clazz = entityTypeDescriptors.get( 0 ).getJavaClass();
					partitionBounds.add( new PartitionBound( clazz, null, null, IndexScope.HQL ) );
					break;

				case CRITERIA:
					partitionBounds = buildPartitionUnitsFrom( ss, entityTypeDescriptors.get( 0 ),
							jobData.getCustomQueryCriteria(), maxResults, idFetchSize, rowsPerPartition,
							IndexScope.CRITERIA );
					break;

				case FULL_ENTITY:
					for ( EntityTypeDescriptor entityTypeDescriptor : entityTypeDescriptors ) {
						partitionBounds.addAll( buildPartitionUnitsFrom( ss, entityTypeDescriptor,
								Collections.emptySet(), maxResults, idFetchSize, rowsPerPartition,
								IndexScope.FULL_ENTITY ) );
					}
					break;
			}

			// Build partition plan
			final int partitions = partitionBounds.size();
			final Properties[] props = new Properties[partitions];

			for ( int i = 0; i < partitionBounds.size(); i++ ) {
				PartitionBound bound = partitionBounds.get( i );
				props[i] = new Properties();
				props[i].setProperty( MassIndexingPartitionProperties.ENTITY_NAME, bound.getEntityName() );
				props[i].setProperty( MassIndexingPartitionProperties.PARTITION_ID, String.valueOf( i ) );
				props[i].setProperty( MassIndexingPartitionProperties.LOWER_BOUND, SerializationUtil.serialize( bound.getLowerBound() ) );
				props[i].setProperty( MassIndexingPartitionProperties.UPPER_BOUND, SerializationUtil.serialize( bound.getUpperBound() ) );
				props[i].setProperty( MassIndexingPartitionProperties.INDEX_SCOPE, bound.getIndexScope().name() );
			}

			log.infof( "Partitions: %s", (Object) props );

			PartitionPlan partitionPlan = new PartitionPlanImpl();
			partitionPlan.setPartitionProperties( props );
			partitionPlan.setPartitions( partitions );
			if ( StringHelper.isNotEmpty( serializedMaxThreads ) ) {
				final int threads = SerializationUtil.parseIntegerParameter( MAX_THREADS, serializedMaxThreads );
				partitionPlan.setThreads( threads );
			}

			log.partitionsPlan( partitionPlan.getPartitions(), partitionPlan.getThreads() );
			return partitionPlan;
		}
		finally {
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
		}
	}

	private List<PartitionBound> buildPartitionUnitsFrom(StatelessSession ss,
			EntityTypeDescriptor entityTypeDescriptor, Set<Criterion> customQueryCriteria,
			Integer maxResults, int fetchSize, int rowsPerPartition,
			IndexScope indexScope) {
		Class<?> javaClass = entityTypeDescriptor.getJavaClass();
		List<PartitionBound> partitionUnits = new ArrayList<>();

		Object lowerID = null;
		Object upperID = null;

		Criteria criteria = ss.createCriteria( javaClass );
		entityTypeDescriptor.getIdOrder().addAscOrder( criteria );

		if ( maxResults != null ) {
			criteria.setMaxResults( maxResults );
		}

		ScrollableResults scroll = criteria.setProjection( Projections.id() )
				.setFetchSize( fetchSize )
				.setReadOnly( true )
				.scroll( ScrollMode.FORWARD_ONLY );

		try {
			/*
			 * The scroll results are originally positioned *before* the first element,
			 * so we need to scroll rowsPerPartition + 1 positions to advanced to the
			 * upper bound of the first partition, whereas for the next partitions
			 * we only need to advance rowsPerPartition positions.
			 * This handle the special case of the first partition.
			 */
			scroll.next();

			while ( scroll.scroll( rowsPerPartition ) ) {
				lowerID = upperID;
				upperID = scroll.get( 0 );
				partitionUnits.add( new PartitionBound( javaClass, lowerID, upperID, indexScope ) );
			}

			// add an additional partition on the tail
			lowerID = upperID;
			upperID = null;
			partitionUnits.add( new PartitionBound( javaClass, lowerID, upperID, indexScope ) );
			return partitionUnits;
		}
		finally {
			try {
				if ( scroll != null ) {
					scroll.close();
				}
			}
			catch (Exception e) {
				log.unableToCloseScrollableResults( e );
			}
		}
	}

}
