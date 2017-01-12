/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Properties;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.runtime.context.JobContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.steps.lucene.PartitionMapper;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for partition plan validation.
 *
 * @author Mincong Huang
 */
public class PartitionMapperTest {

	private static final Logger LOGGER = Logger.getLogger( PartitionMapperTest.class );
	private static final int COMP_ROWS = 3;
	private static final int PERS_ROWS = 8;
	private EntityManagerFactory emf;

	@Mock
	private JobContext mockedJobContext;

	@InjectMocks
	private PartitionMapper partitionMapper;

	@Before
	public void setUp() {
		EntityManager em = null;
		try {
			emf = Persistence.createEntityManagerFactory( "h2" );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			for ( int i = 1; i <= COMP_ROWS; i++ ) {
				em.persist( new Company( "C" + i ) );
			}
			for ( int i = 1; i <= PERS_ROWS; i++ ) {
				em.persist( new Person( "P" + i, "", "" ) );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}

		final String fetchSize = String.valueOf( 200 * 1000 );
		final String hql = null;
		final String maxThreads = String.valueOf( 1 );
		final String rowsPerPartition = String.valueOf( 3 );
		partitionMapper = new PartitionMapper( null,
				fetchSize,
				hql,
				rowsPerPartition,
				maxThreads );

		MockitoAnnotations.initMocks( this );
	}

	/**
	 * Prove that there're N + 1 partitions for each root entity, where N stands for the ceiling number of the division
	 * between the rows to index and the max rows per partition, and the 1 stands for the tail partition for entities
	 * inserted after partitioning.
	 *
	 * @throws Exception
	 */
	@Test
	public void testMapPartitions() throws Exception {

		// mock job context
		JobContextData jobData = new JobContextData();
		jobData.setEntityManagerFactory( emf );
		jobData.setCriteria( new HashSet<>() );
		jobData.setEntityTypes( Company.class, Person.class );
		Mockito.when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		PartitionPlan partitionPlan = partitionMapper.mapPartitions();

		int compPartitions = 0;
		int persPartitions = 0;
		for ( Properties p : partitionPlan.getPartitionProperties() ) {
			String entityName = p.getProperty( "entityName" );
			if ( entityName.equals( Company.class.getName() ) ) {
				compPartitions++;
			}
			if ( entityName.equals( Person.class.getName() ) ) {
				persPartitions++;
			}
		}

		// nbPartitions = rows / rowsPerPartition + 1
		assertEquals( 2, compPartitions ); // 3 / 3 + 1 = 2 partitions
		assertEquals( 3, persPartitions ); // 8 / 3 + 1 = 3 partitions
	}

	@After
	public void shutDown() {
		if ( emf.isOpen() ) {
			emf.close();
		}
	}
}
