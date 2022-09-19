/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Properties;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.runtime.context.JobContext;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.massindexing.impl.JobContextData;
import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.HibernateSearchPartitionMapper;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.MassIndexingPartitionProperties;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Person;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
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

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();
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
		setupContext.withAnnotatedTypes( Company.class, Person.class )
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false );
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

		final String fetchSize = String.valueOf( 200 * 1000 );
		final String hql = null;
		final String maxThreads = String.valueOf( 1 );
		final String rowsPerPartition = String.valueOf( 3 );

		mockedJobContext = mock( JobContext.class );
		partitionMapper = new HibernateSearchPartitionMapper(
				fetchSize,
				hql,
				maxThreads,
				null,
				rowsPerPartition,
				null,
				null,
				mockedJobContext
		);
	}

	/**
	 * Prove that there're N partitions for each root entity,
	 * where N stands for the ceiling number of the division
	 * between the rows to index and the max rows per partition.
	 */
	@Test
	public void testMapPartitions() throws Exception {
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		jobData.setEntityTypeDescriptors( Arrays.asList(
				JobTestUtil.createSimpleEntityTypeDescriptor( emf, Company.class ),
				JobTestUtil.createSimpleEntityTypeDescriptor( emf, Person.class )
				) );
		when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compPartitions = 0;
		int persPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( MassIndexingPartitionProperties.ENTITY_NAME );
			if ( entityName.equals( Company.class.getName() ) ) {
				compPartitions++;
			}
			if ( entityName.equals( Person.class.getName() ) ) {
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
}
