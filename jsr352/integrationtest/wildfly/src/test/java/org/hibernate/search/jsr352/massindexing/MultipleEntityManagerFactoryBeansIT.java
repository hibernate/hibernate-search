/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.massindexing.BatchIndexingJob;
import org.hibernate.search.jsr352.massindexing.test.common.Message;
import org.hibernate.search.jsr352.massindexing.test.common.MessageManager;
import org.hibernate.search.jsr352.massindexing.test.config.MultipleEntityManagerFactoriesProducer;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
public class MultipleEntityManagerFactoryBeansIT {

	private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = MultipleEntityManagerFactoriesProducer.H2_ENTITY_MANAGER_FACTORY_BEAN_NAME;

	private static final int JOB_TIMEOUT_MS = 40_000;

	private static final SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yyyy", Locale.ROOT );
	private static final int DB_DAY1_ROWS = 2000;
	private static final int DB_DAY2_ROWS = 3000;

	@Inject
	private MessageManager messageManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, MultipleEntityManagerFactoryBeansIT.class.getSimpleName() + ".war" )
				.addAsResource( "META-INF/persistence_multiple.xml", "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/make-deployment-as-batch-app.xml" ) // WFLY-7000
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() )
				.addClass( MultipleEntityManagerFactoriesProducer.class );
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

		JobOperator jobOperator = BatchRuntime.getJobOperator();

		// The 1st execution. Keep it alive and wait Byteman to stop it
		long execId1 = BatchIndexingJob.forEntity( Message.class )
				.entityManagerFactoryReference( ENTITY_MANAGER_FACTORY_BEAN_NAME )
				.start();
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );

		assertEquals( BatchStatus.COMPLETED, jobExec1.getBatchStatus() );
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}
}
