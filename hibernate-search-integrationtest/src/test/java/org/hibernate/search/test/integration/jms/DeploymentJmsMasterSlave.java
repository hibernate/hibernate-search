package org.hibernate.search.test.integration.jms;

import static org.hibernate.search.test.integration.jms.util.RegistrationConfiguration.indexLocation;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.hibernate.search.test.integration.jms.controller.RegistrationController;
import org.hibernate.search.test.integration.jms.controller.RegistrationMdb;
import org.hibernate.search.test.integration.jms.model.RegisteredMember;
import org.hibernate.search.test.integration.jms.util.RegistrationConfiguration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.spec.jpa.persistence.PersistenceUnitDef;

/**
 * Create deployments for JMS Master/Slave configuration integration tests.
 *
 * @author "Davide D'Alto"
 *
 */
public class DeploymentJmsMasterSlave {

	public static Archive<?> createMaster(String deploymentName, int refreshPeriod) throws Exception {
		return baseArchive( deploymentName, masterPersistenceXml(deploymentName, refreshPeriod) )
				.addClass( RegistrationMdb.class )
				.addAsWebInfResource( hornetqJmsXml(), "hornetq-jms.xml" )
				;
	}

	public static Archive<?> createSlave(String deploymentName, int refreshPeriod) throws Exception {
		return baseArchive( deploymentName, slavePersitenceXml( deploymentName, refreshPeriod ) );
	}

	private static WebArchive baseArchive(String name, PersistenceUnitDef unitDef) throws Exception {
		return ShrinkWrap
				.create( WebArchive.class, name + ".war" )
				.addClasses( RegistrationController.class, RegisteredMember.class, RegistrationConfiguration.class )
				.addAsResource( new StringAsset( unitDef.exportAsString() ), "META-INF/persistence.xml" )
				.addAsLibraries( libraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				;
	}

	private static PersistenceUnitDef masterPersistenceXml(String name, int refreshPeriod) throws Exception {
		return commonUnitDef( "pu-" + name, "filesystem-master", refreshPeriod )
				.property( "hibernate.search.default.indexBase", indexLocation( "index-master" ) )
				;
	}

	private static PersistenceUnitDef slavePersitenceXml(String name, int refreshPeriod) throws Exception {
		return commonUnitDef( "pu-" + name, "filesystem-slave", refreshPeriod )
				.property( "hibernate.search.default.worker.backend", "jms" )
				.property( "hibernate.search.default.worker.jms.connection_factory", "ConnectionFactory" )
				.property( "hibernate.search.default.worker.jms.queue", RegistrationConfiguration.DESTINATION_QUEUE )
				.property( "hibernate.search.default.indexBase", indexLocation( callerName() + "-" + name ) )
				;
	}

	private static String callerName() {
		try {
			throw new RuntimeException();
		} catch (Exception ex ) {
			return ex.getStackTrace()[3].getClassName();
		}
	}

	private static PersistenceUnitDef commonUnitDef(String unitName, String directoryProvider, int refreshPeriod) throws Exception {
		return Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.persistenceUnit( unitName )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.property( "hibernate.hbm2ddl.auto", "create-drop" )
				.property( "hibernate.search.default.lucene_version", "LUCENE_CURRENT" )
				.property( "hibernate.search.default.directory_provider", directoryProvider )
				.property( "hibernate.search.default.sourceBase", indexLocation( "sourceBase" ) )
				.property( "hibernate.search.default.refresh", refreshPeriod )
				.property( "hibernate.search.default.worker.execution", "sync" )
				;
	}

	private static Collection<JavaArchive> libraries() throws Exception {
		Collection<JavaArchive> libs = new ArrayList<JavaArchive>();
		File libDir = new File(getLibrariesDirectory());
		for ( String lib : libDir.list() ) {
			JavaArchive javaArchive = ShrinkWrap
					.create( ZipImporter.class, lib )
					.importFrom( new File( libDir, lib ) )
					.as( JavaArchive.class )
					;
			libs.add( javaArchive );
		}
		return libs;
	}

	private static Asset hornetqJmsXml() {
		String hornetqXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
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

	private static String getLibrariesDirectory() throws Exception {
		InputStream resourceAsStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream( "integration-test.properties" );
		Properties properties = new Properties();
		properties.load( resourceAsStream );
		return properties.getProperty( "lib.dir" );
	}
}
