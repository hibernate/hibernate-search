/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.jakarta.batch.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.runtime.context.JobContext;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Company;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.CompanyGroup;
import org.hibernate.search.integrationtest.jakarta.batch.massindexing.entity.Person;
import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.impl.JobContextData;
import org.hibernate.search.jakarta.batch.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * Single-component test for partition plan validation.
 *
 * @author Mincong Huang
 */
public class HibernateSearchPartitionMapperComponentIT {

	private static final int COMP_ROWS = 3;
	private static final int PERS_ROWS = 8;

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private EntityManagerFactory emf;

	private JobContext mockedJobContext;

	private HibernateSearchPartitionMapper partitionMapper;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( Company.class, Person.class, CompanyGroup.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false );
	}

	@Before
	public void init() {
		emf = setupHolder.entityManagerFactory();

		setupHolder.runInTransaction( session -> {
			for ( int i = 1; i <= COMP_ROWS; i++ ) {
				session.persist( new Company( "C" + i ) );
			}
			for ( int i = 1; i <= PERS_ROWS; i++ ) {
				session.persist( new Person( "P" + i, "", "" ) );
			}
		} );

		final String maxThreads = String.valueOf( 1 );
		final String rowsPerPartition = String.valueOf( 3 );

		mockedJobContext = mock( JobContext.class );
		partitionMapper = new HibernateSearchPartitionMapper(
				null, null,
				maxThreads,
				null,
				rowsPerPartition,
				null,
				null,
				mockedJobContext
		);
	}

	/**
	 * Prove that there are N partitions for each root entity,
	 * where N stands for the ceiling number of the division
	 * between the rows to index and the max rows per partition.
	 */
	@Test
	public void simple() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		var companyType = JobTestUtil.createEntityTypeDescriptor( emf, Company.class );
		var personType = JobTestUtil.createEntityTypeDescriptor( emf, Person.class );
		jobData.setEntityTypeDescriptors( Arrays.asList( companyType, personType ) );
		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compPartitions = 0;
		int persPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( MassIndexingPartitionProperties.ENTITY_NAME );
			if ( entityName.equals( companyType.jpaEntityName() ) ) {
				compPartitions++;
			}
			if ( entityName.equals( personType.jpaEntityName() ) ) {
				persPartitions++;
			}
			/*
			 * The checkpoint interval should have defaulted to the value of rowsPerPartition,
			 * since the value of rowsPerPartition is lower than the static default for checkpoint interval.
			 */
			String checkpointInterval = p.getProperty( MassIndexingPartitionProperties.CHECKPOINT_INTERVAL );
			assertNotNull( checkpointInterval );
			assertEquals( "3", checkpointInterval );
		}

		// nbPartitions = rows / rowsPerPartition
		assertEquals( 1, compPartitions ); // 3 / 3 => 1 partition
		assertEquals( 3, persPartitions ); // 8 / 3 => 3 partitions
	}

	@Test
	public void noData() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		var companyGroupType = JobTestUtil.createEntityTypeDescriptor( emf, CompanyGroup.class );
		jobData.setEntityTypeDescriptors( Collections.singletonList( companyGroupType ) );
		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compGroupPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( MassIndexingPartitionProperties.ENTITY_NAME );
			if ( entityName.equals( companyGroupType.jpaEntityName() ) ) {
				compGroupPartitions++;
			}
		}

		// Did not find anything in the ResultSet at index "rowsPerPartition"
		// => 1 partition covering the whole range.
		// We'll notice there is no data later, when reading IDs to reindex.
		assertEquals( 1, compGroupPartitions );
	}
}
