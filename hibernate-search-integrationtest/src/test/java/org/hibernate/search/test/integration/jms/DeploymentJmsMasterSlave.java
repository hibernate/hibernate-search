package org.hibernate.search.test.integration.jms;

import static org.hibernate.search.test.integration.jms.util.RegistrationConfiguration.indexLocation;

import java.io.File;

import org.hibernate.search.Version;
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
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * Create deployments for JMS Master/Slave configuration integration tests.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
public class DeploymentJmsMasterSlave {

	private static File[] libraryFiles;

	public static Archive<?> createMaster(String deploymentName, int refreshPeriod) throws Exception {
		return baseArchive( deploymentName, masterPersistenceXml(deploymentName, refreshPeriod) )
				.addClass( RegistrationMdb.class )
				.addAsWebInfResource( hornetqJmsXml(), "hornetq-jms.xml" )
				;
	}

	public static Archive<?> createSlave(String deploymentName, int refreshPeriod) throws Exception {
		return baseArchive( deploymentName, slavePersitenceXml( deploymentName, refreshPeriod ) );
	}

	private static WebArchive baseArchive(String name, PersistenceDescriptor unitDef) throws Exception {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, name + ".war" )
				.addClasses( RegistrationController.class, RegisteredMember.class, RegistrationConfiguration.class )
				.addAsResource( new StringAsset( unitDef.exportAsString() ), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				;
		addLibraries( webArchive );
		return webArchive;
	}

	private static PersistenceDescriptor masterPersistenceXml(String name, int refreshPeriod) throws Exception {
		return commonUnitDef( "pu-" + name, "filesystem-master", refreshPeriod )
				.createProperty().name( "hibernate.search.default.indexBase" ).value( indexLocation( "index-master" ) ).up()
				.up().up();
	}

	private static PersistenceDescriptor slavePersitenceXml(String name, int refreshPeriod) throws Exception {
		return commonUnitDef( "pu-" + name, "filesystem-slave", refreshPeriod )
				.createProperty().name( "hibernate.search.default.worker.backend" ).value( "jms" ).up()
				.createProperty().name( "hibernate.search.default.worker.jms.connection_factory" ).value( "ConnectionFactory" ).up()
				.createProperty().name( "hibernate.search.default.worker.jms.queue" ).value( RegistrationConfiguration.DESTINATION_QUEUE ).up()
				.createProperty().name( "hibernate.search.default.indexBase" ).value( indexLocation( callerName() + "-" + name ) ).up()
				.up().up();
	}

	private static String callerName() {
		try {
			throw new RuntimeException();
		} catch (Exception ex ) {
			return ex.getStackTrace()[3].getClassName();
		}
	}

	private static Properties<PersistenceUnit<PersistenceDescriptor>> commonUnitDef(String unitName, String directoryProvider, int refreshPeriod) throws Exception {
		return Descriptors.create( PersistenceDescriptor.class )
				.createPersistenceUnit()
					.name( unitName )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( directoryProvider ).up()
					.createProperty().name( "hibernate.search.default.sourceBase" ).value( indexLocation( "sourceBase" ) ).up()
					.createProperty().name( "hibernate.search.default.refresh" ).value( String.valueOf( refreshPeriod ) ).up()
					.createProperty().name( "hibernate.search.default.worker.execution" ).value( "sync" ).up();
	}

	private static void addLibraries(WebArchive archive) {
		if ( libraryFiles == null ) { //cache this as Maven resolution is painfully slow
			MavenDependencyResolver resolver = DependencyResolvers
				.use( MavenDependencyResolver.class )
				.goOffline();
			String currentVersion = Version.getVersionString();
			libraryFiles = resolver
						.artifact( "org.hibernate:hibernate-search-orm:" + currentVersion )
							.exclusion( "org.hibernate:hibernate-entitymanager" )
							.exclusion( "org.hibernate:hibernate-core" )
							.exclusion( "org.hibernate:hibernate-search-analyzers" )
							.exclusion( "org.hibernate.common:hibernate-commons-annotations" )
							.exclusion( "org.jboss.logging:jboss-logging" )
							.exclusion( "org.slf4j:slf4j-api" )
						.resolveAsFiles();
		}
		archive.addAsLibraries( libraryFiles );
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

}
