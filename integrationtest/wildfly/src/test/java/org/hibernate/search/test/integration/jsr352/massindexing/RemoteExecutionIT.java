/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jsr352.massindexing;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;

import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.Message;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.MessageManager;
import org.hibernate.search.test.integration.jsr352.massindexing.test.util.JobInterruptorUtil;
import org.hibernate.search.test.integration.jsr352.massindexing.test.util.ManagementClientJobTestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

/**
 * Test remote execution of the Hibernate Search JSR-352 batch job.
 * <p>
 * <strong>Important:</strong> Note that methods are executed in sequence,
 * some of the methods being executed in-container (because it's easier for data initialization and such)
 * and some others being executed as client (because that's what we want to test).
 */
@RunWith(Arquillian.class)
public class RemoteExecutionIT {
	private static final String DEPLOYMENT_ARCHIVE_NAME = RemoteExecutionIT.class.getSimpleName() + ".war";

	private static final PathAddress JBERET_SUBSYSTEM_ADDRESS =
			PathAddress.pathAddress( "deployment", DEPLOYMENT_ARCHIVE_NAME )
					.append( "subsystem", "batch-jberet" );

	private static final int JOB_TIMEOUT_MS = 40_000;

	private static final Date DB_DAY1;
	private static final int DB_DAY1_ROWS = 2000;
	private static final Date DB_DAY2;
	private static final int DB_DAY2_ROWS = 3000;
	static {
		SimpleDateFormat format = new SimpleDateFormat( "dd/MM/yyyy", Locale.ROOT );
		try {
			DB_DAY1 = format.parse( "31/08/2016" );
			DB_DAY2 = format.parse( "01/09/2016" );
		}
		catch (ParseException e) {
			throw new IllegalStateException( e );
		}
	}

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, DEPLOYMENT_ARCHIVE_NAME )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-jsr352-wildflycontroller.xml", "/jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobInterruptorUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() )
				.addClasses( ManagementClient.class );
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

	@Inject
	private MessageManager messageManager;

	/*
	 * Cannot be in a @Before because @Before is executed on both sides
	 * (in the client and in the container).
 	 */
	@Test
	@InSequence(0)
	public void insertData() {
		List<Message> messages = new LinkedList<>();
		for ( int i = 0; i < DB_DAY1_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), DB_DAY1 ) );
		}
		for ( int i = 0; i < DB_DAY2_ROWS; i++ ) {
			messages.add( new Message( String.valueOf( i ), DB_DAY2 ) );
		}
		messageManager.persist( messages );
	}

	@Test
	@InSequence(1)
	public void enableInterruptor() {
		// Simulate a failure for the next execution
		JobInterruptorUtil.enable();
	}

	@Test
	@InSequence(2)
	public void checkIndexEmpty() {
		assertEquals( 0, messageManager.findMessagesFor( DB_DAY1 ).size() );
		assertEquals( 0, messageManager.findMessagesFor( DB_DAY2 ).size() );
	}

	@Test
	@InSequence(3)
	@RunAsClient
	public void startAndFail(@ArquillianResource ManagementClient managementClient) throws InterruptedException, IOException {
		ManagementClientJobTestUtil jobTestUtil = new ManagementClientJobTestUtil(
				managementClient, JBERET_SUBSYSTEM_ADDRESS, MassIndexingJob.NAME
		);

		long execId1 = jobTestUtil.start(
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.build()
		);
		BatchStatus executionStatus = jobTestUtil.waitForTermination( execId1, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.FAILED, executionStatus );
	}

	@Test
	@InSequence(4)
	public void disableInterruptor() {
		// Do not simulate any failure for the next execution
		JobInterruptorUtil.disable();
	}

	@Test
	@InSequence(5)
	@RunAsClient
	public void restartAndSucceed(@ArquillianResource ManagementClient managementClient) throws InterruptedException, IOException {
		ManagementClientJobTestUtil jobTestUtil = new ManagementClientJobTestUtil(
				managementClient, JBERET_SUBSYSTEM_ADDRESS, MassIndexingJob.NAME
		);
		// Restart the job. This is the 2nd execution.
		long execId2 = jobTestUtil.restart( jobTestUtil.getLastExecutionId().get(), null );
		BatchStatus executionStatus = jobTestUtil.waitForTermination( execId2, JOB_TIMEOUT_MS );
		assertEquals( BatchStatus.COMPLETED, executionStatus );
	}

	@Test
	@InSequence(6)
	public void checkIndexPopulated() {
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( DB_DAY1 ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( DB_DAY2 ).size() );
	}

	@Test
	@InSequence(7)
	public void removeAll() {
		messageManager.removeAll();
	}

}
