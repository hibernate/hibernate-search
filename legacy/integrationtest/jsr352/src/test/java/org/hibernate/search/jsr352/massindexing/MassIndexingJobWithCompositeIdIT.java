/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.massindexing.test.entity.EntityWithEmbeddedId;
import org.hibernate.search.jsr352.massindexing.test.entity.EntityWithIdClass;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.jsr352.test.util.PersistenceUnitTestUtil;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that mass indexing job can handle entity having
 * {@link javax.persistence.EmbeddedId} annotation, or
 * {@link javax.persistence.IdClass} annotation.
 *
 * @author Mincong Huang
 */
@TestForIssue(jiraKey = "HSEARCH-2615")
public class MassIndexingJobWithCompositeIdIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	private static final int JOB_TIMEOUT_MS = 30_000;

	private static final LocalDate START = LocalDate.of( 2017, 6, 1 );

	private static final LocalDate END = LocalDate.of( 2017, 8, 1 );

	private EntityManagerFactory emf;
	private FullTextEntityManager ftem;

	@Before
	public void setUp() throws Exception {
		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );

		ftem = Search.getFullTextEntityManager( emf.createEntityManager() );
		ftem.getTransaction().begin();
		for ( LocalDate d = START; d.isBefore( END ); d = d.plusDays( 1 ) ) {
			ftem.persist( new EntityWithIdClass( d ) );
			ftem.persist( new EntityWithEmbeddedId( d ) );
		}
		ftem.getTransaction().commit();

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class ) ).isEqualTo( 0 );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class ) ).isEqualTo( 0 );
	}

	@After
	public void tearDown() throws Exception {
		ftem.getTransaction().begin();

		ftem.createQuery( "delete from " + EntityWithIdClass.class.getSimpleName() ).executeUpdate();
		ftem.createQuery( "delete from " + EntityWithEmbeddedId.class.getSimpleName() ).executeUpdate();

		ftem.purgeAll( EntityWithIdClass.class );
		ftem.purgeAll( EntityWithEmbeddedId.class );
		ftem.flushToIndexes();

		ftem.getTransaction().commit();
		ftem.close();

		emf.close();
	}

	@Test
	public void canHandleIdClass_strategyFull() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithIdClass.class )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( START, END );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class ) ).isEqualTo( expectedDays );
	}

	@Test
	public void canHandleIdClass_strategyCriteria() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithIdClass.class )
				.restrictedBy( Restrictions.gt( "month", 6 ) )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	@Test
	public void canHandleEmbeddedId_strategyFull() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithEmbeddedId.class )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();

		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( START, END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	@Test
	public void canHandleEmbeddedId_strategyCriteria() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithEmbeddedId.class )
				.restrictedBy( Restrictions.gt( "embeddableDateId.month", 6 ) )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

}
