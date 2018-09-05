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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.Message;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.MessageManager;
import org.hibernate.search.test.integration.jsr352.massindexing.test.config.MultipleEntityManagerFactoriesProducer;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

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
 * Test the behavior when there are multiple entity manager factories (persistence units),
 * but those are correctly registered as CDI beans.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultipleEntityManagerFactoriesRegisteredAsBeansIT {

	private static final String ENTITY_MANAGER_FACTORY_BEAN_NAME = MultipleEntityManagerFactoriesProducer.PRIMARY_ENTITY_MANAGER_FACTORY_BEAN_NAME;

	private static final int JOB_TIMEOUT_MS = 40_000;

	private static final SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yyyy", Locale.ROOT );
	private static final int DB_DAY1_ROWS = 2000;
	private static final int DB_DAY2_ROWS = 3000;

	@Inject
	private MessageManager messageManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, MultipleEntityManagerFactoriesRegisteredAsBeansIT.class.getSimpleName() + ".war" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-jsr352.xml", "/jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() )
				.addClass( MultipleEntityManagerFactoriesProducer.class );
		return war;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( MultipleEntityManagerFactoriesProducer.PRIMARY_PERSISTENCE_UNIT_NAME )
				.jtaDataSource( WildFlyConfigurationExtension.DATA_SOURCE_JNDI_NAME )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "hibernate.search.indexing_strategy" ).value( "manual" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.createPersistenceUnit()
				.name( MultipleEntityManagerFactoriesProducer.UNUSED_PERSISTENCE_UNIT_NAME )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
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
		List<Message> messages = new ArrayList<>( DB_DAY1_ROWS + DB_DAY2_ROWS );
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
		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.entityManagerFactoryReference( ENTITY_MANAGER_FACTORY_BEAN_NAME )
						.build()
				);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );

		assertEquals( BatchStatus.COMPLETED, jobExec1.getBatchStatus() );
		assertEquals( DB_DAY1_ROWS, messageManager.findMessagesFor( SDF.parse( "31/08/2016" ) ).size() );
		assertEquals( DB_DAY2_ROWS, messageManager.findMessagesFor( SDF.parse( "01/09/2016" ) ).size() );
	}
}
