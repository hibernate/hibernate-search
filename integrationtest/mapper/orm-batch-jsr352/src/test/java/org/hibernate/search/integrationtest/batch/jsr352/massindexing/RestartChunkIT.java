/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.SimulatedFailureCompany;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.util.SimulatedFailure;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class RestartChunkIT {

	private static final int CHECKPOINT_INTERVAL = 10;

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	private static final int JOB_TIMEOUT_MS = 10_000;

	protected static final long DB_COMP_ROWS = 150;

	private JobOperator jobOperator;
	private EntityManagerFactory emf;

	@Before
	public void setup() {
		jobOperator = JobTestUtil.getAndCheckRuntime();

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
			em.persist( new SimulatedFailureCompany( str[i % 5] + "-" + i ) );
		}
		em.getTransaction().commit();
		em.close();
	}

	@After
	public void shutdownJPA() {
		emf.close();
	}

	@Test
	public void failureBeforeFirstRead_fullScope() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionOnNextRead();
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	public void failureDuringFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 0.5 ) );
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	public void failureDuringNonFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 2.5 ) );
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	public void failureBeforeFirstRead_hql() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionOnNextRead();
		doTest( "select c from SimulatedFailureCompany c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	public void failureDuringFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 0.5 ) );
		doTest( "select c from SimulatedFailureCompany c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	public void failureDuringNonFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException, IOException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 2.5 ) );
		doTest( "select c from SimulatedFailureCompany c where c.name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	private void doTest(String hql, long expectedTotal, long expectedGoogle) throws InterruptedException, IOException {
		SearchWorkspace workspace = Search.mapping( emf ).scope( SimulatedFailureCompany.class ).workspace();
		workspace.purge();
		workspace.refresh();
		workspace.flush();

		assertEquals( 0, JobTestUtil.nbDocumentsInIndex( emf, SimulatedFailureCompany.class ) );
		List<SimulatedFailureCompany> google = JobTestUtil.findIndexedResults( emf, SimulatedFailureCompany.class, "name", "Google" );
		assertEquals( 0, google.size() );

		// start the job
		MassIndexingJob.ParametersBuilder builder = MassIndexingJob.parameters()
				.forEntities( SimulatedFailureCompany.class );
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
		// job will be stopped by the SimulatedFailure
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
		assertEquals( expectedTotal, JobTestUtil.nbDocumentsInIndex( emf, SimulatedFailureCompany.class ) );
		google = JobTestUtil.findIndexedResults( emf, SimulatedFailureCompany.class, "name", "google" );
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
