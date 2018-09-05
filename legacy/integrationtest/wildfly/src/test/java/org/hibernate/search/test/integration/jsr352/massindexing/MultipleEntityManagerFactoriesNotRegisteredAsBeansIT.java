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

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.jsr352.test.util.JobTestUtil;
import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.jsr352.massindexing.test.common.Message;
import org.hibernate.search.test.integration.jsr352.massindexing.test.config.MultipleEntityManagerFactoriesProducer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the behavior when there are multiple entity manager factories (persistence units),
 * and they haven't been registered as CDI beans.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class MultipleEntityManagerFactoriesNotRegisteredAsBeansIT {

	private static final String PERSISTENCE_UNIT_NAME = MultipleEntityManagerFactoriesProducer.PRIMARY_PERSISTENCE_UNIT_NAME;

	private static final int JOB_TIMEOUT_MS = 40_000;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap
				.create( WebArchive.class, MultipleEntityManagerFactoriesNotRegisteredAsBeansIT.class.getSimpleName() + ".war" )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-jsr352.xml", "/jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( JobTestUtil.class.getPackage() )
				.addPackage( Message.class.getPackage() );
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

	@Test
	public void testJob() throws InterruptedException, IOException, ParseException {
		JobOperator jobOperator = BatchRuntime.getJobOperator();

		long execId1 = jobOperator.start(
				MassIndexingJob.NAME,
				MassIndexingJob.parameters()
						.forEntity( Message.class )
						.entityManagerFactoryReference( PERSISTENCE_UNIT_NAME )
						.build()
				);
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		JobTestUtil.waitForTermination( jobOperator, jobExec1, JOB_TIMEOUT_MS );

		/*
		 * We expect failure, because we can only retrieve the default PU by default
		 * (unless a non-default namespace is used, but that requires user configuration).
		 */
		assertEquals( BatchStatus.FAILED, jobExec1.getBatchStatus() );
	}
}
