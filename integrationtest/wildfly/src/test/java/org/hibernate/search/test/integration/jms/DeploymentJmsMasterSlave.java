/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.jms;

import java.io.File;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.controller.RegistrationMdb;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
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

	public static Archive<?> createSlave(String deploymentName, int refreshPeriod, File tmpDir) throws Exception {
		return baseArchive( deploymentName, slavePersistenceXml( deploymentName, refreshPeriod, tmpDir ) );
	}

	private static WebArchive baseArchive(String name, PersistenceDescriptor unitDef) throws Exception {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, name + ".war" )
				.addClasses( RegistrationController.class, RegisteredMember.class, RegistrationConfiguration.class, SearchNewEntityJmsMasterSlave.class )
				.addAsResource( new StringAsset( unitDef.exportAsString() ), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return webArchive;
	}

	private static PersistenceDescriptor masterPersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-master", refreshPeriod, tmpDir ).up().up();
	}

	private static PersistenceDescriptor slavePersistenceXml(String name, int refreshPeriod, File tmpDir)
			throws Exception {
		return commonUnitDef( name, "filesystem-slave", refreshPeriod, tmpDir )
					.createProperty()
						.name( "hibernate.search.default.worker.backend" )
						.value( "jms" )
						.up()
					//We could use a Local ConnectionFactory but then we would bypass the authentication:
					//we actually want to verify we're able to authenticate
					.createProperty()
						.name( "hibernate.search.default.worker.jms.connection_factory" )
						.value( "jboss/exported/jms/RemoteConnectionFactory" )
						.up()
					.createProperty()
						.name( "hibernate.search.default.worker.jms.queue" )
						.value( RegistrationConfiguration.DESTINATION_QUEUE )
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
					.getOrCreateProperties()
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
							+ "<entry name=\"" + RegistrationConfiguration.DESTINATION_QUEUE + "\"/>"
						+ "</jms-queue>"
					+ "</jms-destinations>"
				+ "</hornetq-server>"
			+ "</messaging-deployment>";
		return new StringAsset( hornetqXml );
	}
}
