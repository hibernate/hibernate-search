/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.impl.steps.lucene.StepProgress;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.massindexing.test.entity.CompanyGroup;
import org.hibernate.search.jsr352.massindexing.test.entity.Person;
import org.hibernate.search.jsr352.massindexing.test.entity.WhoAmI;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;

/**
 * @author Mincong Huang
 */
public class BatchIndexingJobIT extends AbstractBatchIndexingIT {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int JOB_TIMEOUT_MS = 30_000;

	/*
	 * Make sure to have more than one checkpoint,
	 * because we had errors related to that in the past.
	 */
	private static final int CHECKPOINT_INTERVAL = INSTANCES_PER_DATA_TEMPLATE / 2;

	private static final String MAIN_STEP_NAME = "produceLuceneDoc";

	@Test
	public void simple() throws InterruptedException,
			IOException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, people.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, whos.size() );
	}

	@Test
	public void simple_defaultCheckpointInterval() throws InterruptedException,
			IOException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, people.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, whos.size() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2637")
	public void indexedEmbeddedCollection() throws InterruptedException,
			IOException {
		EntityManager em = null;

		try {
			em = emf.createEntityManager();
			em.getTransaction().begin();
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			CriteriaQuery<Company> criteria = criteriaBuilder.createQuery( Company.class );
			Root<Company> root = criteria.from( Company.class );
			Path<Integer> id = root.get( root.getModel().getId( int.class ) );
			criteria.orderBy( criteriaBuilder.asc( id ) );
			List<Company> companies = em.createQuery( criteria ).getResultList();
			List<CompanyGroup> groups = new ArrayList<>();
			for ( int i = 0; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
				int index1 = i;
				int index2 = i + 1;
				int index3 = i + 2;
				groups.add( new CompanyGroup( "group" + index1, companies.get( index1 ) ) );
				groups.add( new CompanyGroup( "group" + index2, companies.get( index1 ), companies.get( index2 ) ) );
				groups.add( new CompanyGroup( "group" + index3, companies.get( index3 ) ) );
			}
			groups.forEach( em::persist );
			em.getTransaction().commit();
		}
		finally {
			if ( em != null ) {
				em.close();
			}
		}

		List<CompanyGroup> groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		List<CompanyGroup> groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		List<CompanyGroup> groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertEquals( 0, groupsContainingGoogle.size() );
		assertEquals( 0, groupsContainingRedHat.size() );
		assertEquals( 0, groupsContainingMicrosoft.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( CompanyGroup.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, CompanyGroup.class, INSTANCE_PER_ENTITY_TYPE );

		groupsContainingGoogle = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Google" );
		groupsContainingRedHat = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Red Hat" );
		groupsContainingMicrosoft = JobTestUtil.findIndexedResults( emf, CompanyGroup.class, "companies.name", "Microsoft" );
		assertEquals( 2 * INSTANCES_PER_DATA_TEMPLATE, groupsContainingGoogle.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, groupsContainingRedHat.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, groupsContainingMicrosoft.size() );
	}

	@Test
	public void criteria() throws InterruptedException,
			IOException {
		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.restrictedBy( Restrictions.or(
								Restrictions.like( "name", "Google%" ),
								Restrictions.like( "name","Red Hat%" )
						) )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( INSTANCES_PER_DATA_TEMPLATE, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );
	}

	@Test
	public void purge() throws InterruptedException, IOException {
		int expectedCount = 10;

		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
		indexSomeCompanies( expectedCount );
		assertEquals( expectedCount, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );

		/*
		 * Request a mass indexing with a filter matching nothing,
		 * which should effectively amount to a simple purge.
		 */
		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.purgeAllOnStart( true )
						.restrictedBy(
								Restrictions.eq( "name", "NEVER_MATCH" )
						)
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
	}

	@Test
	public void noPurge() throws InterruptedException, IOException {
		int expectedCount = 10;

		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
		indexSomeCompanies( expectedCount );
		assertEquals( expectedCount, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );

		/*
		 * Request a mass indexing with a filter matching nothing, and requesting no purge at all,
		 * which should effectively amount to a no-op.
		 */
		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.purgeAllOnStart( false )
						.restrictedBy(
								Restrictions.eq( "name", "NEVER_MATCH" )
						)
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( expectedCount, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
	}

	@Test
	public void hql() throws InterruptedException,
			IOException {
		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.restrictedBy( "select c from Company c where c.name like 'Google%' or c.name like 'Red Hat%'" )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( INSTANCES_PER_DATA_TEMPLATE, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" ).size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Red Hat" ).size() );
		assertEquals( 0, JobTestUtil.findIndexedResults( emf, Company.class, "name", "Microsoft" ).size() );
	}

	@Test
	public void criteria_maxResults() throws InterruptedException,
			IOException {
		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );

		int maxResults = CHECKPOINT_INTERVAL + 1;

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.restrictedBy( Restrictions.or(
								Restrictions.like( "name", "Google%" ),
								Restrictions.like( "name","Red Hat%" )
						) )
						.maxResultsPerEntity( maxResults )
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( maxResults, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
	}

	@Test
	public void hql_maxResults() throws InterruptedException,
			IOException {
		// searches before mass index,
		// expected no results for each search
		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );

		int maxResults = CHECKPOINT_INTERVAL + 1;

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.restrictedBy( "select c from Company c where c.name like 'Google%' or c.name like 'Red Hat%'" )
						.maxResultsPerEntity( maxResults )
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		assertEquals( maxResults, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
	}

	@Test
	public void partitioned() throws InterruptedException,
			IOException {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		List<Person> people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		List<WhoAmI> whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );
		assertEquals( 0, whos.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntities( Company.class, Person.class, WhoAmI.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.rowsPerPartition( INSTANCE_PER_ENTITY_TYPE - 1 )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );
		assertCompletion( executionId );
		assertProgress( executionId, Person.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, Company.class, INSTANCE_PER_ENTITY_TYPE );
		assertProgress( executionId, WhoAmI.class, INSTANCE_PER_ENTITY_TYPE );

		StepProgress progress = getMainStepProgress( executionId );
		Map<Integer, Long> partitionProgress = progress.getPartitionProgress();
		assertThat( partitionProgress )
				.as( "Entities processed per partition" )
				.hasSize( 3 * 2 )
				.contains(
						// First entity type
						entry( 0, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for first entity type
						entry( 1, 1L ), // Partition 2 for first entity type
						// Second entity type
						entry( 2, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for second entity type
						entry( 3, 1L ), // Partition 2 for second entity type
						// Third entity type
						entry( 4, INSTANCE_PER_ENTITY_TYPE - 1L ), // Partition 1 for third entity type
						entry( 5, 1L ) // Partition 2 for third entity type
				);

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		people = JobTestUtil.findIndexedResults( emf, Person.class, "firstName", "Linus" );
		whos = JobTestUtil.findIndexedResults( emf, WhoAmI.class, "id", "id01" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, people.size() );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, whos.size() );
	}

	private void assertCompletion(long executionId) {
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			BatchStatus batchStatus = stepExecution.getBatchStatus();
			log.infof( "step %s executed.", stepExecution.getStepName() );
			assertEquals( BatchStatus.COMPLETED, batchStatus );
		}
	}

	private void assertProgress(long executionId, Class<?> entityType, int progressValue) {
		/*
		 * We cannot check the metrics, which in JBatch are set to 0
		 * for partitioned steps (the metrics are handled separately for
		 * each partition).
		 * Thus we check our own object.
		 */
		StepProgress progress = getMainStepProgress( executionId );
		assertEquals( Long.valueOf( progressValue ), progress.getEntityProgress().get( entityType.getName() ) );
	}

	private StepProgress getMainStepProgress(long executionId) {
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			switch ( stepExecution.getStepName() ) {
				case MAIN_STEP_NAME:
					return (StepProgress) stepExecution.getPersistentUserData();
				default:
					break;
			}
		}
		throw new AssertionFailure( "Missing step progress for step '" + MAIN_STEP_NAME + "'" );
	}

}
