/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import java.io.File;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.integration.VersionTestHelper;
import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.controller.RegistrationMdb;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.test.integration.jms.transaction.TransactionalJmsMasterSlave;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

/**
 * Create deployments for JMS Master/Slave configuration integration tests.
 * Make sure to test for a secured JMS environment.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public final class DeploymentJmsMasterSlave {

	private DeploymentJmsMasterSlave() {
		//not allowed
	}

	public static Archive<?> createMaster(String deploymentName, int refreshPeriod, File tmpDir) throws Exception {
		return baseArchive( deploymentName, masterPersistenceXml( deploymentName, refreshPeriod, tmpDir ) )
				.addClass( RegistrationMdb.class )
				.addAsWebInfResource( hornetqJmsXml(), "hornetq-jms.xml" )
				;
	}

	public static Archive<?> createSlave(String deploymentName, int refreshPeriod, File tmpDir, boolean transactional) throws Exception {
		return baseArchive( deploymentName, slavePersistenceXml( deploymentName, refreshPeriod, tmpDir, transactional ) );
	}

	private static WebArchive baseArchive(String name, PersistenceDescriptor unitDef) throws Exception {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, name + ".war" )
				.addClasses(
						RegistrationController.class,
						RegisteredMember.class,
						RegistrationConfiguration.class,
						MasterSlaveTestTemplate.class,
						TransactionalJmsMasterSlave.class
				)
				.addAsResource( new StringAsset( unitDef.exportAsString() ), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return webArchive;
	}

	private static PersistenceDescriptor masterPersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-master", refreshPeriod, tmpDir ).up().up();
	}

	private static PersistenceDescriptor slavePersistenceXml(String name, int refreshPeriod, File tmpDir, Boolean transactional)
			throws Exception {
		String jmsConnectionFactory = transactional ? "java:jboss/DefaultJMSConnectionFactory" : "jboss/exported/jms/RemoteConnectionFactory";
		return commonUnitDef( name, "filesystem-slave", refreshPeriod, tmpDir )
					.createProperty()
						.name( "hibernate.search.default.worker.backend" )
						.value( "jms" )
						.up()
					.createProperty()
						.name( Environment.WORKER_ENLIST_IN_TRANSACTION )
						.value( transactional.toString() )
						.up()
					//We could use a Local ConnectionFactory but then we would bypass the authentication:
					//we actually want to verify we're able to authenticate
					.createProperty()
						.name( "hibernate.search.default.worker.jms.connection_factory" )
						.value( jmsConnectionFactory )
						.up()
					.createProperty()
						.name( "hibernate.search.default.worker.jms.queue" )
						.value( RegistrationMdb.DESTINATION_QUEUE )
						.up()
					//Authentication credentials are specified in the AS7 configuration files
					//See properties files in server/standalone/configuration
					.createProperty()
						.name( "hibernate.search.default.worker.jms.login" )
						.value( "guest" )
						.up()
					.createProperty()
						.name( "hibernate.search.default.worker.jms.password" )
						.value( "password" )
						.up()
					.up()
				.up();
	}

	private static Properties<PersistenceUnit<PersistenceDescriptor>> commonUnitDef(
			String name, String directoryProvider, int refreshPeriod, File tmpDir) throws Exception {
		return Descriptors.create( PersistenceDescriptor.class )
				.createPersistenceUnit()
					.name( "pu-" + name )
					.jtaDataSource( "java:jboss/datasources/ExampleDS" )
					// The deployment Scanner is disabled as the JipiJapa integration is not available because of the custom Hibernate ORM module:
					.clazz( RegisteredMember.class.getName() )
					.getOrCreateProperties()
						.createProperty()
							.name( "jboss.as.jpa.providerModule" )
							.value( VersionTestHelper.injectVariables( "org.hibernate.search.hibernate-orm-repackage:${dependency.version.HibernateSearch}" ) )
							.up()
						.createProperty()
							.name( "hibernate.transaction.jta.platform" )
							.value( "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform" )
							.up()
						.createProperty()
							.name( "hibernate.hbm2ddl.auto" )
							.value( "create-drop" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.lucene_version" )
							.value( "LUCENE_CURRENT" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.directory_provider" )
							.value( directoryProvider )
							.up()
						.createProperty()
							.name( "hibernate.search.default.sourceBase" )
							.value( tmpDir.getAbsolutePath() + "-sourceBase" )
							.up()
						.createProperty()
							.name( "hibernate.search.default.indexBase" )
							.value( tmpDir.getAbsolutePath() + "-" + name )
							.up()
						.createProperty()
							.name( "hibernate.search.default.refresh" )
							.value( String.valueOf( refreshPeriod ) )
							.up()
						.createProperty()
							.name( "hibernate.search.default.worker.execution" )
							.value( "sync" )
							.up();
	}

	private static Asset hornetqJmsXml() {
		String hornetqXml =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<messaging-deployment xmlns=\"urn:jboss:messaging-deployment:1.0\">"
				+ "<hornetq-server>"
					+ "<jms-destinations>"
						+ "<jms-queue name=\"hsearchQueue\">"
							+ "<entry name=\"" + RegistrationMdb.DESTINATION_QUEUE + "\"/>"
						+ "</jms-queue>"
					+ "</jms-destinations>"
				+ "</hornetq-server>"
			+ "</messaging-deployment>";
		return new StringAsset( hornetqXml );
	}
}
