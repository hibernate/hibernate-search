/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.util.List;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.jsr352.massindexing.test.entity.Company;
import org.hibernate.search.jsr352.test.util.JobTestUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mincong Huang
 */
public class EntityManagerFactoryRetrievalIT extends AbstractBatchIndexingIT {

	/*
	 * Make sure to have more than one checkpoint,
	 * because we had errors related to that in the past.
	 */
	private static final int CHECKPOINT_INTERVAL = 10;

	private static final String SESSION_FACTORY_NAME = "primary_session_factory";

	private static final int JOB_TIMEOUT_MS = 10_000;

	@Test
	public void defaultNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, companies.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryReference( getPersistenceUnitName() )
						.build()
		);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
	}

	@Test
	public void persistenceUnitNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, companies.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryNamespace( "persistence-unit-name" )
						.entityManagerFactoryReference( getPersistenceUnitName() )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
	}

	@Test
	public void sessionFactoryNamespace() throws Exception {
		List<Company> companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( 0, companies.size() );

		long executionId = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Company.class )
						.checkpointInterval( CHECKPOINT_INTERVAL )
						.entityManagerFactoryNamespace( "session-factory-name" )
						.entityManagerFactoryReference( SESSION_FACTORY_NAME )
						.build()
				);
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		JobTestUtil.waitForTermination( jobOperator, jobExecution, JOB_TIMEOUT_MS );

		companies = JobTestUtil.findIndexedResults( emf, Company.class, "name", "Google" );
		assertEquals( INSTANCES_PER_DATA_TEMPLATE, companies.size() );
	}

}
