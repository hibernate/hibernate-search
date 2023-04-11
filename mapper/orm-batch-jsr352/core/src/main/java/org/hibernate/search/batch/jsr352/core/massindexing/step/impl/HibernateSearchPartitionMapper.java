/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.step.impl;

import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.CHECKPOINT_INTERVAL;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.ID_FETCH_SIZE;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.MAX_THREADS;
import static org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.ROWS_PER_PARTITION;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionPlanImpl;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.query.Query;
import org.hibernate.search.batch.jsr352.core.logging.impl.Log;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.PartitionBound;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
public class HibernateSearchPartitionMapper implements PartitionMapper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.TENANT_ID)
	private String tenantId;

	private EntityManagerFactory emf;

	public HibernateSearchPartitionMapper() {
	}

	/**
	 * Constructor for unit test.
	 */
	public HibernateSearchPartitionMapper(
				String serializedIdFetchSize,
				String customQueryHql,
				String serializedMaxThreads,
				String serializedMaxResultsPerEntity,
				String serializedRowsPerPartition,
				String serializedCheckpointInterval,
				String tenantId,
				JobContext jobContext) {
		this.serializedIdFetchSize = serializedIdFetchSize;
		this.customQueryHql = customQueryHql;
		this.serializedMaxThreads = serializedMaxThreads;
		this.serializedMaxResultsPerEntity = serializedMaxResultsPerEntity;
		this.serializedRowsPerPartition = serializedRowsPerPartition;
		this.serializedCheckpointInterval = serializedCheckpointInterval;
		this.tenantId = tenantId;
		this.jobContext = jobContext;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		emf = jobData.getEntityManagerFactory();

		try ( StatelessSession ss = PersistenceUtil.openStatelessSession( emf, tenantId ) ) {
			Integer maxResults = SerializationUtil.parseIntegerParameterOptional(
					MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity, null
			);
			int rowsPerPartition = SerializationUtil.parseIntegerParameterOptional(
					ROWS_PER_PARTITION, serializedRowsPerPartition, Defaults.ROWS_PER_PARTITION
			);
			Integer checkpointIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
					CHECKPOINT_INTERVAL, serializedCheckpointInterval, null
			);
			int checkpointInterval = Defaults.checkpointInterval( checkpointIntervalRaw, rowsPerPartition );
			int idFetchSize = SerializationUtil.parseIntegerParameterOptional(
					ID_FETCH_SIZE, serializedIdFetchSize, Defaults.ID_FETCH_SIZE
			);

			List<EntityTypeDescriptor> entityTypeDescriptors = jobData.getEntityTypeDescriptors();
			List<PartitionBound> partitionBounds = new ArrayList<>();

			switch ( PersistenceUtil.getIndexScope( customQueryHql ) ) {
				case HQL:
					Class<?> clazz = entityTypeDescriptors.get( 0 ).getJavaClass();
					partitionBounds.add( new PartitionBound( clazz, null, null, IndexScope.HQL ) );
					break;

				case FULL_ENTITY:
					for ( EntityTypeDescriptor entityTypeDescriptor : entityTypeDescriptors ) {
						partitionBounds.addAll( buildPartitionUnitsFrom( emf, ss, entityTypeDescriptor,
								maxResults, idFetchSize, rowsPerPartition,
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
				props[i].setProperty(
						MassIndexingPartitionProperties.CHECKPOINT_INTERVAL,
						String.valueOf( checkpointInterval )
				);
			}

			log.infof( "Partitions: %s", (Object) props );

			PartitionPlan partitionPlan = new PartitionPlanImpl();
			partitionPlan.setPartitionProperties( props );
			partitionPlan.setPartitions( partitions );
			Integer threads = SerializationUtil.parseIntegerParameterOptional(
					MAX_THREADS, serializedMaxThreads, null
			);
			if ( threads != null ) {
				partitionPlan.setThreads( threads );
			}

			log.partitionsPlan( partitionPlan.getPartitions(), partitionPlan.getThreads() );
			return partitionPlan;
		}
	}

	@SuppressWarnings("unchecked") // Can't do much better without adding generics to EntityTypeDescriptor
	private List<PartitionBound> buildPartitionUnitsFrom(EntityManagerFactory emf, StatelessSession ss,
			EntityTypeDescriptor entityTypeDescriptor,
			Integer maxResults, int fetchSize, int rowsPerPartition,
			IndexScope indexScope) {
		Class<?> javaClass = entityTypeDescriptor.getJavaClass();
		List<PartitionBound> partitionUnits = new ArrayList<>();

		Object lowerID;
		Object upperID = null;

		CriteriaBuilder builder = emf.getCriteriaBuilder();
		CriteriaQuery<?> criteria = builder.createQuery();
		Root<?> root = criteria.from( javaClass );

		entityTypeDescriptor.getIdOrder().addAscOrder( builder, criteria, root );

		EntityType<?> model = root.getModel();
		Class<?> javaType = model.getIdType().getJavaType();

		@SuppressWarnings("rawtypes")
		SingularAttribute singularAttribute = model.getId( javaType );
		criteria.select( root.get( singularAttribute ) );

		Query<?> query = ss.createQuery( criteria );

		query.setFetchSize( fetchSize )
				.setReadOnly( true )
				.setCacheable( false )
				.setLockMode( LockModeType.NONE );

		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}

		try ( ScrollableResults scroll = query.scroll( ScrollMode.SCROLL_SENSITIVE ) ) {
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
				upperID = scroll.get();
				partitionUnits.add( new PartitionBound( javaClass, lowerID, upperID, indexScope ) );
			}

			// add an additional partition on the tail
			lowerID = upperID;
			upperID = null;
			partitionUnits.add( new PartitionBound( javaClass, lowerID, upperID, indexScope ) );
			return partitionUnits;
		}
	}

}
