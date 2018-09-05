/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.hibernate.criterion.Restrictions;
import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.Message;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.MessageManager;
import org.hibernate.search.test.integration.jsr352.massindexing.test.util.JobInterruptorUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;

/**
 * This integration test (IT) aims to test the restartability of the job execution mass-indexer under Java EE
 * environment, with step partitioning (parallelism). We need to prove that the job restart from the checkpoint where it
 * was stopped, but not from the very beginning.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class RestartIT {

	private static final int JOB_TIMEOUT_MS = 40_000;

	private static final SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yyyy", Locale.ROOT );
	private static final int DB_DAY1_ROWS = 2000;
	private static final int DB_DAY2_ROWS = 3000;

	@Inject
	private MessageManager messageManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, RestartIT.class.getSimpleName() + ".war" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-jsr352.xml", "/jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( JobInterruptorUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() );
		return war;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( MessageManager.PERSISTENCE_UNIT_NAME )
				.jtaDataSource( WildFlyConfigurationExtension.DATA_SOURCE_JNDI_NAME )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "hibernate.search.indexing_strategy" ).value( "manual" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Before
	public void insertData() throws ParseException {
		List<Message> messages = new LinkedList<>();
		for ( int i = 0; i < DB_DAY1_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), SDF.parse( "31/08/2016" ) ) );
		}
		for ( int i = 0; i < DB_DAY2_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), SDF.parse( "01/09/2016" ) ) );
		}
		messageManager.persist( messages );
	}

	@After
	public void removeAll() {
		messageManager.removeAll();
	}

	@Test
	public void testJob() throws InterruptedException, IOException, ParseException {
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		JobOperator jobOperator = BatchRuntime.getJobOperator();

		// The 1st execution. Keep it alive and wait Byteman to stop it
		JobInterruptorUtil.enable();
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.build()
				);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
		JobInterruptorUtil.disable();

		// Restart the job. This is the 2nd execution.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = JobTestUtil.waitForTermination( jobOperator, jobExec2, JOB_TIMEOUT_MS );

		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

	@Test
	public void testJob_usingCriteria() throws InterruptedException, IOException, ParseException {
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		JobOperator jobOperator = BatchRuntime.getJobOperator();

		// The 1st execution. Keep it alive and wait Byteman to stop it
		JobInterruptorUtil.enable();
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.restrictedBy( Restrictions.ge( "date", SDF.parse( "01/09/2016" ) ) )
						.build()
				);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
		JobInterruptorUtil.disable();

		// Restart the job. This is the 2nd execution.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = JobTestUtil.waitForTermination( jobOperator, jobExec2, JOB_TIMEOUT_MS );

		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

	@Test
	public void testJob_usingHQL() throws Exception {
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );

		JobOperator jobOperator = BatchRuntime.getJobOperator();

		JobInterruptorUtil.enable();
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.restrictedBy( "select m from Message m where day( m.date ) = 1" )
						.build()
				);
		JobExecution jobExec1 = BatchRuntime.getJobOperator().getJobExecution( execId1 );
		jobExec1 = JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
		JobInterruptorUtil.disable();

		// Restart the job.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		JobTestUtil.waitForTermination( jobOperator, jobExec2, JOB_TIMEOUT_MS );

		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );
		assertEquals( 0, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}

}
