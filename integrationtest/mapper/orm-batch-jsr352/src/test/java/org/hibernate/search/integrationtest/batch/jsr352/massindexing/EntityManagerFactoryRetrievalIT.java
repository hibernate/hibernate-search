/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.batch.jsr352.massindexing;

import static org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil.JOB_TIMEOUT_MS;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.JobExecution;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJob;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Company;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.Person;
import org.hibernate.search.integrationtest.batch.jsr352.massindexing.entity.WhoAmI;
import org.hibernate.search.integrationtest.batch.jsr352.util.JobTestUtil;
import org.hibernate.search.integrationtest.batch.jsr352.util.PersistenceUnitTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class EntityManagerFactoryRetrievalIT {

	private static final String PERSISTENCE_UNIT_NAME = PersistenceUnitTestUtil.getPersistenceUnitName();

	protected static final int INSTANCES_PER_DATA_TEMPLATE = 100;

	// We have three data templates per entity type (see setup)
	protected static final int INSTANCE_PER_ENTITY_TYPE = INSTANCES_PER_DATA_TEMPLATE * 3;

	/*
	 * Make sure to have more than one checkpoint,
	 * because we had errors related to that in the past.
	 */
	private static final int CHECKPOINT_INTERVAL = 10;

	private static final String SESSION_FACTORY_NAME = "primary_session_factory";

	protected JobOperator jobOperator;
	protected EntityManagerFactory emf;

	@Before
	public void setup() {
		jobOperator = JobTestUtil.getAndCheckRuntime();
		List<Company> companies = new ArrayList<>();
		List<Person> people = new ArrayList<>();
		List<WhoAmI> whos = new ArrayList<>();
		for ( int i = 0; i < INSTANCE_PER_ENTITY_TYPE; i += 3 ) {
			int index1 = i;
			int index2 = i + 1;
			int index3 = i + 2;
			companies.add( new Company( "Google " + index1 ) );
			companies.add( new Company( "Red Hat " + index2 ) );
			companies.add( new Company( "Microsoft " + index3 ) );
			people.add( new Person( "BG " + index1, "Bill", "Gates" ) );
			people.add( new Person( "LT " + index2, "Linus", "Torvalds" ) );
			people.add( new Person( "SJ " + index3, "Steven", "Jobs" ) );
			whos.add( new WhoAmI( "cid01 " + index1, "id01 " + index1, "uid01 " + index1 ) );
			whos.add( new WhoAmI( "cid02 " + index2, "id02 " + index2, "uid02 " + index2 ) );
			whos.add( new WhoAmI( "cid03 " + index3, "id03 " + index3, "uid03 " + index3 ) );
		}

		emf = Persistence.createEntityManagerFactory( getPersistenceUnitName() );
		with( emf ).runInTransaction( em -> {
			companies.forEach( em::persist );
			people.forEach( em::persist );
			whos.forEach( em::persist );
		} );
	}

	@After
	public void shutdown() {
		if ( emf != null ) {
			emf.close();
		}
	}

	protected String getPersistenceUnitName() {
		return PERSISTENCE_UNIT_NAME;
	}

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
