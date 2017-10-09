/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.massindexing.MassIndexingJob.ParametersBuilder;
import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.jsr352.test.util.PersistenceUnitTestUtil;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;

import static org.junit.Assert.assertEquals;

/**
 * @author Mincong Huang
 */
@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
public class RestartChunkIT {

	private static final int CHECKPOINT_INTERVAL = 10;

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	private static final int JOB_TIMEOUT_MS = 10_000;

	protected static final long DB_COMP_ROWS = 150;

	private JobOperator jobOperator = BatchRuntime.getJobOperator();
	private EntityManagerFactory emf;

	@Before
	public void setup() {
		String[] str = new String[] {
				"Google",
				"Red Hat",
				"Microsoft",
				"Facebook",
				"Amazon"
		};

		emf = Persistence.createEntityManagerFactory( PERSISTENCE_UNIT_NAME );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			em.persist( new Company( str[i % 5] + "-" + i ) );
		}
		em.getTransaction().commit();
		em.close();
	}

	@After
	public void shutdownJPA() {
		emf.close();
	}

	@Test
	@BMRule(
			name = "Fail before the first read",
			targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader",
			targetMethod = "readItem",
			targetLocation = "AT ENTRY",
			helper = BytemanHelper.NAME,
			condition = "flag(\"failureBeforeFirstRead_fullScope.failed\")",
			action = "simulateFailure()"
	)
	public void failureBeforeFirstRead_fullScope() throws InterruptedException, IOException {
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	@BMRules(rules = {
			@BMRule(
					name = "Create count-down before the step partitioning",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.PartitionMapper",
					targetMethod = "mapPartitions",
					targetLocation = "AT EXIT",
					// The counter value must be less than CHECKPOINT_INTERVAL, but non-zero
					action = "createCountDown(\"failureDuringFirstCheckpointBetweenTwoWrites_fullScope.countDown\", " + (int) ( CHECKPOINT_INTERVAL * 0.5 ) + ")"
			),
			@BMRule(
					name = "Count down for each item written, simulate failure when counter is 0",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.LuceneDocWriter",
					targetMethod = "writeItem",
					targetLocation = "AT EXIT",
					helper = BytemanHelper.NAME,
					condition = "countDown(\"failureDuringFirstCheckpointBetweenTwoWrites_fullScope.countDown\")"
							+ " && flag(\"failureDuringFirstCheckpointBetweenTwoWrites_fullScope.failed\")",
					action = "simulateFailure()"
			)
	})
	public void failureDuringFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException, IOException {
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	@BMRules(rules = {
			@BMRule(
					name = "Create count-down before the step partitioning",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.PartitionMapper",
					targetMethod = "mapPartitions",
					targetLocation = "AT EXIT",
					// The counter value must NOT be a multiple of CHECKPOINT_INTERVAL
					action = "createCountDown(\"failureDuringCheckpointBetweenTwoWrites_fullScope.countDown\", " + (int) ( CHECKPOINT_INTERVAL * 2.5 ) + ")"
			),
			@BMRule(
					name = "Count down for each item written, simulate failure when counter is 0",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.LuceneDocWriter",
					targetMethod = "writeItem",
					targetLocation = "AT EXIT",
					helper = BytemanHelper.NAME,
					condition = "countDown(\"failureDuringCheckpointBetweenTwoWrites_fullScope.countDown\")"
							+ " && flag(\"failureDuringCheckpointBetweenTwoWrites_fullScope.failed\")",
					action = "simulateFailure()"
			)
	})
	public void failureDuringNonFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException, IOException {
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@BMRule(
			name = "Fail before the first read",
			targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader",
			targetMethod = "readItem",
			targetLocation = "AT ENTRY",
			helper = BytemanHelper.NAME,
			condition = "flag(\"failureBeforeFirstRead_hql.failed\")",
			action = "simulateFailure()"
	)
	public void failureBeforeFirstRead_hql() throws InterruptedException, IOException {
		doTest( "select c from Company c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	@BMRules(rules = {
			@BMRule(
					name = "Create count-down before the step partitioning",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.PartitionMapper",
					targetMethod = "mapPartitions",
					targetLocation = "AT EXIT",
					// The counter value must be less than CHECKPOINT_INTERVAL, but non-zero
					action = "createCountDown(\"failureDuringFirstCheckpointBetweenTwoWrites_hql.countDown\", " + (int) ( CHECKPOINT_INTERVAL * 0.5 ) + ")"
			),
			@BMRule(
					name = "Count down for each item written, simulate failure when counter is 0",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.LuceneDocWriter",
					targetMethod = "writeItem",
					targetLocation = "AT EXIT",
					helper = BytemanHelper.NAME,
					condition = "countDown(\"failureDuringFirstCheckpointBetweenTwoWrites_hql.countDown\")"
							+ " && flag(\"failureDuringFirstCheckpointBetweenTwoWrites_hql.failed\")",
					action = "simulateFailure()"
			)
	})
	public void failureDuringFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException, IOException {
		doTest( "select c from Company c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	@BMRules(rules = {
			@BMRule(
					name = "Create count-down before the step partitioning",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.PartitionMapper",
					targetMethod = "mapPartitions",
					targetLocation = "AT EXIT",
					// The counter value must NOT be a multiple of CHECKPOINT_INTERVAL
					action = "createCountDown(\"failureDuringCheckpointBetweenTwoWrites_hql.countDown\", " + (int) ( CHECKPOINT_INTERVAL * 2.5 ) + ")"
			),
			@BMRule(
					name = "Count down for each item written, simulate failure when counter is 0",
					targetClass = "org.hibernate.search.jsr352.massindexing.impl.steps.lucene.LuceneDocWriter",
					targetMethod = "writeItem",
					targetLocation = "AT EXIT",
					helper = BytemanHelper.NAME,
					condition = "countDown(\"failureDuringCheckpointBetweenTwoWrites_hql.countDown\")"
							+ " && flag(\"failureDuringCheckpointBetweenTwoWrites_hql.failed\")",
					action = "simulateFailure()"
			)
	})
	public void failureDuringNonFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException, IOException {
		doTest( "select c from Company c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	private void doTest(String hql, long expectedTotal, long expectedGoogle) throws InterruptedException, IOException {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();
		ftem.close();

		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
		List<Company> google = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, google.size() );

		// start the job
		ParametersBuilder builder = MassIndexingJob.parameters()
				.forEntities( Company.class );
		if ( hql != null ) {
			builder = builder.restrictedBy( hql );
		}
		Properties parameters = builder
				.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
				.checkpointInterval( CHECKPOINT_INTERVAL )
				.build();
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				parameters
		);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );
		// job will be stopped by the byteman
		assertEquals( BatchStatus.FAILED, getMainStepStatus( execId1 ) );

		// restart the job
		/*
		 * From the specs (v1.0, 10.8.1):
		 * Job parameter values are not remembered from one execution to the next.
		 */
		long execId2 = jobOperator.restart( execId1, parameters );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		JobTestUtil.waitForTermination( jobOperator, jobExec2, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.COMPLETED, getMainStepStatus( execId2 ) );

		// search again
		assertEquals( expectedTotal, JobTestUtil.nbDocumentsInIndex( emf, Company.class ) );
		google = JobTestUtil.findIndexedResults( emf, Company.class, "name", "google" );
		assertEquals( expectedGoogle, google.size() );
	}

	private Object getMainStepStatus(long execId1) {
		for ( StepExecution stepExec : jobOperator.getStepExecutions( execId1 ) ) {
			if ( stepExec.getStepName().equals( "produceLuceneDoc" ) ) {
				return stepExec.getBatchStatus();
			}
		}
		return null;
	}

}
