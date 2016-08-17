/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.test.entity.Company;
import org.hibernate.search.jsr352.test.entity.CompanyManager;
import org.hibernate.search.jsr352.test.entity.Person;
import org.hibernate.search.jsr352.test.entity.PersonManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This integration test (IT) aims to test the restartability of the job
 * execution mass-indexer under Java EE environment, with step partitioning
 * (parallelism). We need to prove that the job restart from the checkpoint
 * where it was stopped, but not from the very beginning.
 *
 * @author Mincong Huang
 */
@Ignore("No need to run another restart test under Java EE")
@RunWith(Arquillian.class)
public class RestartIT {

	private static final Logger LOGGER = Logger.getLogger( RestartIT.class );

	private final boolean JOB_PURGE_AT_START = true;
	private final int JOB_FETCH_SIZE = 100 * 1000;
	private final int JOB_MAX_RESULTS = 200 * 1000;
	private final int JOB_MAX_THREADS = 3;
	private final int JOB_ROWS_PER_PARTITION = 1000;

	private final long DB_COMP_ROWS = 2500;
	private final long DB_PERS_ROWS = 2600;

	private final int MAX_TRIES = 40;
	private final int THREAD_SLEEP = 1000;

	@Inject
	private CompanyManager companyManager;

	@Inject
	private PersonManager personManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/mass-index.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addClasses( Serializable.class, Date.class )
				.addPackages( true, "org.hibernate.search.annotations" )
				.addPackages( true, "org.hibernate.search.jsr352" )
				.addPackages( true, "javax.persistence" );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException {

		final String google = "google";
		final String googleCEO = "Sundar";

		insertData();
		List<Company> googles = companyManager.findCompanyByName( google );
		List<Person> googleCEOs = personManager.findPerson( googleCEO );
		assertEquals( 0, googles.size() );
		assertEquals( 0, googleCEOs.size() );

		// Start the job. This is the 1st execution.
		// Keep the execution alive and wait Byteman to stop the job
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long execId1 = createAndStartJob( jobOperator );
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = keepTestAlive( jobExec1 );

		// Restart the job. This is the 2nd execution.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = keepTestAlive( jobExec2 );
		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );

		googles = companyManager.findCompanyByName( google );
		googleCEOs = personManager.findPerson( googleCEO );
		assertEquals( DB_COMP_ROWS / 5, googles.size() );
		assertEquals( DB_PERS_ROWS / 5, googleCEOs.size() );

		// TODO this method should not belong to company manager
		// but how to create an all context query ?
		int totalDocs = companyManager.findAll().size();
		assertEquals( (int) ( DB_COMP_ROWS + DB_PERS_ROWS ), totalDocs );
	}

	private void insertData() {
		final String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};
		List<Person> people = new ArrayList<>( (int) DB_PERS_ROWS );
		List<Company> companies = new ArrayList<>( (int) DB_COMP_ROWS );
		for ( int i = 0; i < DB_PERS_ROWS; i++ ) {
			Person p = new Person( i, str[i % 5][1], str[i % 5][2] );
			people.add( p );
		}
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			Company c = new Company( str[i % 5][0] );
			companies.add( c );
		}
		personManager.persist( people );
		companyManager.persist( companies );
	}

	private long createAndStartJob(JobOperator jobOperator) {
		MassIndexer massIndexer = new MassIndexer()
				.fetchSize( JOB_FETCH_SIZE )
				.maxResults( JOB_MAX_RESULTS )
				.maxThreads( JOB_MAX_THREADS )
				.purgeAtStart( JOB_PURGE_AT_START )
				.rowsPerPartition( JOB_ROWS_PER_PARTITION )
				.jobOperator( jobOperator )
				.addRootEntities( Company.class, Person.class );
		long executionId = massIndexer.start();
		return executionId;
	}

	private JobExecution keepTestAlive(JobExecution jobExecution)
			throws InterruptedException {

		int tries = 0;
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& tries < MAX_TRIES) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP );
			Thread.sleep( THREAD_SLEEP );
			jobExecution = jobOperator.getJobExecution( executionId );
			tries++;
		}
		return jobExecution;
	}
}
