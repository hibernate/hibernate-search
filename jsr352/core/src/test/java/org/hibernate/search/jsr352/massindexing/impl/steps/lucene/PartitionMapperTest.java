/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Properties;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.runtime.context.JobContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.JobContextData;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.massindexing.test.entity.Person;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( Log.class );

	private static final String PERSISTENCE_UNIT_NAME = "h2";
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
			emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );
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
				log.error( e );
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
		jobData.setCustomQueryCriteria( new HashSet<>() );
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
