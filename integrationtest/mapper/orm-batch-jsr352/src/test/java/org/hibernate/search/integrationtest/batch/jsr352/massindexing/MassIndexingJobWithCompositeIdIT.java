/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.EntityWithEmbeddedId;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.EntityWithIdClass;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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

	@Before
	public void setUp() throws Exception {
		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );

		OrmUtils.withinJPATransaction( emf, ( entityManager ) -> {
			for ( LocalDate d = START; d.isBefore( END ); d = d.plusDays( 1 ) ) {
				entityManager.persist( new EntityWithIdClass( d ) );
				entityManager.persist( new EntityWithEmbeddedId( d ) );
			}
		} );

		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class ) ).isEqualTo( 0 );
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class ) ).isEqualTo( 0 );
	}

	@After
	public void tearDown() throws Exception {
		OrmUtils.withinJPATransaction( emf, ( entityManager ) -> {
			entityManager.createQuery( "delete from " + EntityWithIdClass.class.getSimpleName() )
					.executeUpdate();
			entityManager.createQuery( "delete from " + EntityWithEmbeddedId.class.getSimpleName() )
					.executeUpdate();

			SearchSession searchSession = Search.session( entityManager );
			searchSession.workspace( EntityWithIdClass.class ).purge();
			searchSession.workspace( EntityWithEmbeddedId.class ).purge();
		} );

		emf.close();
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
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
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleIdClass_strategyHql() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithIdClass.class )
				.restrictedBy( "select e from EntityWithIdClass e where e.month = 6" )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithIdClass.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	@Test
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
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
	@Ignore("HSEARCH-4033") // TODO HSEARCH-4033 Support mass-indexing of composite id entities
	public void canHandleEmbeddedId_strategyHql() throws Exception {
		Properties props = MassIndexingJob.parameters()
				.forEntities( EntityWithEmbeddedId.class )
				.restrictedBy( "select e from EntityWithIdClass e where e.embeddableDateId.month = 6" )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}
}
