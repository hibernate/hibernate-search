/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.JOB_TIMEOUT_MS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.util.BackendConfigurations;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.util.SimulatedFailure;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

/**
 * @author Mincong Huang
 */
public class RestartChunkIT {

	private static final int CHECKPOINT_INTERVAL = 10;

	protected static final long DB_COMP_ROWS = 150;

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder =
			ReusableOrmSetupHolder.withSingleBackend( BackendConfigurations.simple() );
	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private EntityManagerFactory emf;
	private JobOperator jobOperator;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		setupContext.withAnnotatedTypes( SimulatedFailureCompany.class )
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false );
	}

	@Before
	public void initData() {
		SimulatedFailure.reset();
		emf = setupHolder.entityManagerFactory();
		jobOperator = JobTestUtil.getAndCheckRuntime();

		String[] str = new String[] {
				"Google",
				"Red Hat",
				"Microsoft",
				"Facebook",
				"Amazon"
		};

		setupHolder.runInTransaction( s -> {
			for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
				s.persist( new SimulatedFailureCompany( str[i % 5] + "-" + i ) );
			}
		} );
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
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

	@Entity(name = "SimulatedFailureCompany")
	@Access(value = AccessType.PROPERTY) // Necessary to hook the getters on EntityWriter phase
	@Indexed
	public static class SimulatedFailureCompany {

		private int id;

		private String name;

		public SimulatedFailureCompany() {
			// Called by Hibernate ORM entity loading, which in turn
			// is called by the EntityReader phase of the batch.
			SimulatedFailure.read();
		}

		public SimulatedFailureCompany(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public int getId() {
			// Called by Hibernate Search indexer#addOrUpdate, which in turn
			// is called by the EntityWriter phase of the batch.
			SimulatedFailure.write();

			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@FullTextField
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "SimulatedFailureCompany [id=" + id + ", name=" + name + "]";
		}

	}
}
