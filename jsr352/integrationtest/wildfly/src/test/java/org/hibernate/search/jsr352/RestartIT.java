/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.test.Message;
import org.hibernate.search.jsr352.test.MessageManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * This integration test (IT) aims to test the restartability of the job execution mass-indexer under Java EE
 * environment, with step partitioning (parallelism). We need to prove that the job restart from the checkpoint where it
 * was stopped, but not from the very beginning.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestartIT {

	private static final Logger LOGGER = Logger.getLogger( RestartIT.class );
	private static final SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yyyy" );
	private static final int DB_DAY1_ROWS = 2000;
	private static final int DB_DAY2_ROWS = 3000;
	private static final int MAX_TRIES = 40;
	private static final int THREAD_SLEEP = 1000;

	@Inject
	private MessageManager messageManager;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, RestartIT.class.getSimpleName() + ".war" )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/make-deployment-as-batch-app.xml" ) // WFLY-7000
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( Message.class.getPackage() );
		return war;
	}

	public void insertData() throws ParseException {
		List<Message> messages = new ArrayList<>( DB_DAY1_ROWS + DB_DAY2_ROWS );
		for ( int i = 0; i < DB_DAY1_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), SDF.parse( "31/08/2016" ) ) );
		}
		for ( int i = 0; i < DB_DAY2_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), SDF.parse( "01/09/2016" ) ) );
		}
		messageManager.persist( messages );
	}

	@Test
	public void testJob() throws InterruptedException, IOException, ParseException {

		insertData();

		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		// The 1st execution. Keep it alive and wait Byteman to stop it
		long execId1 = BatchIndexingJob.forEntity( Message.class ).start();
		JobExecution jobExec1 = BatchRuntime.getJobOperator().getJobExecution( execId1 );
		jobExec1 = keepTestAlive( jobExec1 );

		// Restart the job. This is the 2nd execution.
		long execId2 = BatchIndexingJob.restart( execId1 );
		JobExecution jobExec2 = BatchRuntime.getJobOperator().getJobExecution( execId2 );
		jobExec2 = keepTestAlive( jobExec2 );

		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

	@Test
	public void testJob_usingCriteria() throws InterruptedException, IOException, ParseException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Message.class );
		ftem.flushToIndexes();
		em.close();

		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		// The 1st execution. Keep it alive and wait Byteman to stop it
		long execId1 = BatchIndexingJob.forEntity( Message.class )
				.restrictedBy( Restrictions.ge( "date", SDF.parse( "01/09/2016" ) ) )
				.start();
		JobExecution jobExec1 = BatchRuntime.getJobOperator().getJobExecution( execId1 );
		jobExec1 = keepTestAlive( jobExec1 );

		// Restart the job. This is the 2nd execution.
		long execId2 = BatchIndexingJob.restart( execId1 );
		JobExecution jobExec2 = BatchRuntime.getJobOperator().getJobExecution( execId2 );
		jobExec2 = keepTestAlive( jobExec2 );

		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

	@Test
	public void testJob_usingHQL() throws InterruptedException, IOException, ParseException {

		// purge all before start
		// TODO Can the creation of a new EM and FTEM be avoided?
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.purgeAll( Message.class );
		ftem.flushToIndexes();
		em.close();

		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		long execId1 = BatchIndexingJob.forEntity( Message.class )
				.restrictedBy( "select m from Message m where day( m.date ) = 31" )
				.start();
		JobExecution jobExec1 = BatchRuntime.getJobOperator().getJobExecution( execId1 );
		jobExec1 = keepTestAlive( jobExec1 );

		assertEquals( BatchStatus.COMPLETED, jobExec1.getBatchStatus() );
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

	private JobExecution keepTestAlive(JobExecution jobExecution)
			throws InterruptedException {

		int tries = 0;
		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& tries < MAX_TRIES ) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP );
			Thread.sleep( THREAD_SLEEP );
			jobExecution = BatchRuntime.getJobOperator().getJobExecution( executionId );
			tries++;
		}
		return jobExecution;
	}
}
