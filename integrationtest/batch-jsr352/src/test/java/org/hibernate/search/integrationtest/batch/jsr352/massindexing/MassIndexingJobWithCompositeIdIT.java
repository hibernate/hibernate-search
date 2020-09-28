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
import java.util.function.BiFunction;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.batch.jsr352.core.massindexing.test.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.EntityWithEmbeddedId;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.EntityWithIdClass;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
import org.junit.Before;
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
				.restrictedBy( predicate( (builder, root) -> builder.equal( root.get( "month" ), 6 ),
						EntityWithIdClass.class ) )
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
				.restrictedBy( predicate( (builder, root) -> builder.equal( root.get( "embeddableDateId.month" ), 6 ),
						EntityWithEmbeddedId.class ) )
				.rowsPerPartition( 13 ) // Ensure there're more than 1 partition, so that a WHERE clause is applied.
				.checkpointInterval( 4 )
				.build();
		JobTestUtil.startJobAndWait( MassIndexingJob.NAME, props, JOB_TIMEOUT_MS );

		int expectedDays = (int) ChronoUnit.DAYS.between( LocalDate.of( 2017, 7, 1 ), END );
		int actualDays = JobTestUtil.nbDocumentsInIndex( emf, EntityWithEmbeddedId.class );
		assertThat( actualDays ).isEqualTo( expectedDays );
	}

	private <T> Predicate predicate(BiFunction<CriteriaBuilder, Root<T>, Predicate> predicateFunction, Class<T> type) {
		CriteriaBuilder builder = emf.getCriteriaBuilder();
		Root<T> from = builder.createQuery( type ).from( type );
		return predicateFunction.apply( builder, from );
	}
}
