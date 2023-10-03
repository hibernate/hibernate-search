/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

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

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.SelectionQuery;
import org.hibernate.search.jakarta.batch.core.logging.impl.Log;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PartitionBound;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.PersistenceUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
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
	@BatchProperty(name = MassIndexingJobParameters.ID_FETCH_SIZE)
	private String serializedIdFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_HQL)
	private String reindexOnlyHql;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.REINDEX_ONLY_PARAMETERS)
	private String serializedReindexOnlyParameters;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_THREADS)
	private String serializedMaxThreads;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY)
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
			String reindexOnlyHql,
			String serializedReindexOnlyParameters,
			String serializedMaxThreads,
			String serializedMaxResultsPerEntity,
			String serializedRowsPerPartition,
			String serializedCheckpointInterval,
			String tenantId,
			JobContext jobContext) {
		this.serializedIdFetchSize = serializedIdFetchSize;
		this.reindexOnlyHql = reindexOnlyHql;
		this.serializedReindexOnlyParameters = serializedReindexOnlyParameters;
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
					MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY, serializedMaxResultsPerEntity, null
			);
			int rowsPerPartition = SerializationUtil.parseIntegerParameterOptional(
					MassIndexingJobParameters.ROWS_PER_PARTITION, serializedRowsPerPartition,
					MassIndexingJobParameters.Defaults.ROWS_PER_PARTITION
			);
			Integer checkpointIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
					MassIndexingJobParameters.CHECKPOINT_INTERVAL, serializedCheckpointInterval, null
			);
			int checkpointInterval =
					MassIndexingJobParameters.Defaults.checkpointInterval( checkpointIntervalRaw, rowsPerPartition );
			int idFetchSize = SerializationUtil.parseIntegerParameterOptional(
					MassIndexingJobParameters.ID_FETCH_SIZE, serializedIdFetchSize,
					MassIndexingJobParameters.Defaults.ID_FETCH_SIZE
			);
			ConditionalExpression reindexOnly =
					SerializationUtil.parseReindexOnlyParameters( reindexOnlyHql, serializedReindexOnlyParameters );

			List<EntityTypeDescriptor<?, ?>> entityTypeDescriptors = jobData.getEntityTypeDescriptors();
			List<PartitionBound> partitionBounds = new ArrayList<>();

			for ( EntityTypeDescriptor<?, ?> entityTypeDescriptor : entityTypeDescriptors ) {
				partitionBounds.addAll( buildPartitionUnitsFrom( ss, entityTypeDescriptor,
						maxResults, idFetchSize, rowsPerPartition, reindexOnly ) );
			}

			// Build partition plan
			final int partitions = partitionBounds.size();
			final Properties[] props = new Properties[partitions];

			for ( int i = 0; i < partitionBounds.size(); i++ ) {
				PartitionBound bound = partitionBounds.get( i );
				props[i] = new Properties();
				props[i].setProperty( MassIndexingPartitionProperties.ENTITY_NAME, bound.getEntityName() );
				props[i].setProperty( MassIndexingPartitionProperties.PARTITION_ID, String.valueOf( i ) );
				props[i].setProperty( MassIndexingPartitionProperties.LOWER_BOUND,
						SerializationUtil.serialize( bound.getLowerBound() ) );
				props[i].setProperty( MassIndexingPartitionProperties.UPPER_BOUND,
						SerializationUtil.serialize( bound.getUpperBound() ) );
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
					MassIndexingJobParameters.MAX_THREADS, serializedMaxThreads, null
			);
			if ( threads != null ) {
				partitionPlan.setThreads( threads );
			}

			log.partitionsPlan( partitionPlan.getPartitions(), partitionPlan.getThreads() );
			return partitionPlan;
		}
	}

	private List<PartitionBound> buildPartitionUnitsFrom(StatelessSession ss,
			EntityTypeDescriptor<?, ?> type,
			Integer maxResults, int fetchSize, int rowsPerPartition, ConditionalExpression reindexOnly) {
		List<PartitionBound> partitionUnits = new ArrayList<>();

		Object lowerID;
		Object upperID = null;

		SelectionQuery<?> query = type.createIdentifiersQuery( (SharedSessionContractImplementor) ss,
				reindexOnly == null ? List.of() : List.of( reindexOnly ) )
				.setFetchSize( fetchSize )
				.setReadOnly( true )
				.setCacheable( false )
				.setLockMode( LockModeType.NONE );

		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}

		try ( ScrollableResults<?> scroll = query.scroll( ScrollMode.SCROLL_SENSITIVE ) ) {
			/*
			 * The scroll results are originally positioned *before* the first element,
			 * so we need to scroll rowsPerPartition + 1 positions to advanced to the
			 * upper bound of the first partition, whereas for the next partitions
			 * we need to advance `rowsPerPartition` positions.
			 * The call to scroll.next() handles the special case of the first partition,
			 * as well as the special case where there is no data to index.
			 */
			if ( !scroll.next() ) {
				// Nothing to be indexed.
				return partitionUnits;
			}

			while ( scroll.scroll( rowsPerPartition ) ) {
				lowerID = upperID;
				upperID = scroll.get();
				partitionUnits.add( new PartitionBound( type, lowerID, upperID ) );
			}

			// add an additional partition on the tail
			lowerID = upperID;
			upperID = null;
			partitionUnits.add( new PartitionBound( type, lowerID, upperID ) );
			return partitionUnits;
		}
	}

}
