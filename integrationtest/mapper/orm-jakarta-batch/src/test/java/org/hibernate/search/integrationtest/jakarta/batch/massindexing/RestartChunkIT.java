/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.massindexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Map;
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

import org.hibernate.search.integrationtest.jakarta.batch.util.BackendConfigurations;
import org.hibernate.search.integrationtest.jakarta.batch.util.JobTestUtil;
import org.hibernate.search.integrationtest.jakarta.batch.util.SimulatedFailure;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJob;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Mincong Huang
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestartChunkIT {

	private static final int CHECKPOINT_INTERVAL = 10;

	protected static final long DB_COMP_ROWS = 150;

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory emf;
	private JobOperator jobOperator;

	@BeforeAll
	void setup() {
		emf = ormSetupHelper.start().withAnnotatedTypes( SimulatedFailureCompany.class )
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup();
	}

	@BeforeEach
	void initData() {
		SimulatedFailure.reset();
		jobOperator = JobTestUtil.getOperator();

		String[] str = new String[] {
				"Google",
				"Red Hat",
				"Microsoft",
				"Facebook",
				"Amazon"
		};

		with( emf ).runInTransaction( s -> {
			for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
				s.persist( new SimulatedFailureCompany( str[i % 5] + "-" + i ) );
			}
		} );
	}

	@Test
	void failureBeforeFirstRead_fullScope() throws InterruptedException {
		SimulatedFailure.raiseExceptionOnNextRead();
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	void failureDuringFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 0.5 ) );
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	void failureDuringNonFirstCheckpointBetweenTwoWrites_fullScope() throws InterruptedException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 2.5 ) );
		doTest( null, DB_COMP_ROWS, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	void failureBeforeFirstRead_hql() throws InterruptedException {
		SimulatedFailure.raiseExceptionOnNextRead();
		doTest( "name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	void failureDuringFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 0.5 ) );
		doTest( "name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2616")
	void failureDuringNonFirstCheckpointBetweenTwoWrites_hql() throws InterruptedException {
		SimulatedFailure.raiseExceptionAfterXWrites( (int) ( CHECKPOINT_INTERVAL * 2.5 ) );
		doTest( "name like 'Google%'", DB_COMP_ROWS / 5, DB_COMP_ROWS / 5 );
	}

	private void doTest(String reindexOnly, long expectedTotal, long expectedGoogle) throws InterruptedException {
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, SimulatedFailureCompany.class ) ).isZero();
		List<SimulatedFailureCompany> google = JobTestUtil.findIndexedResults(
				emf, SimulatedFailureCompany.class, "name", "Google" );
		assertThat( google ).isEmpty();

		// start the job
		MassIndexingJob.ParametersBuilder builder = MassIndexingJob.parameters()
				.forEntities( SimulatedFailureCompany.class );
		if ( reindexOnly != null ) {
			builder = builder.reindexOnly( reindexOnly, Map.of() );
		}
		Properties parameters = builder
				.checkpointInterval( CHECKPOINT_INTERVAL )
				.build();
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				parameters
		);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobExec1 );
		// job will be stopped by the SimulatedFailure
		assertThat( getMainStepStatus( execId1 ) ).isEqualTo( BatchStatus.FAILED );

		// restart the job
		/*
		 * From the specs (v1.0, 10.8.1):
		 * Job parameter values are not remembered from one execution to the next.
		 */
		long execId2 = jobOperator.restart( execId1, parameters );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		JobTestUtil.waitForTermination( jobExec2 );
		assertThat( getMainStepStatus( execId2 ) ).isEqualTo( BatchStatus.COMPLETED );

		// search again
		assertThat( JobTestUtil.nbDocumentsInIndex( emf, SimulatedFailureCompany.class ) ).isEqualTo( expectedTotal );
		google = JobTestUtil.findIndexedResults( emf, SimulatedFailureCompany.class, "name", "google" );
		assertThat( google ).hasSize( (int) expectedGoogle );
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
			// is called by the EntityIdReader phase of the batch.
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
