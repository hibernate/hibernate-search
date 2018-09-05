/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.hibernate.search.test.integration.arquillian.WildFlyConfigurationExtension;
import org.hibernate.search.test.integration.wildfly.PackagerHelper;
import org.hibernate.search.test.integration.wildfly.cdi.beans.CDIBeansPackage;
import org.hibernate.search.test.integration.wildfly.cdi.beans.event.BridgeCDILifecycleEventCounter;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;

/**
 * @author Yoann Rodiere
 */
@RunWith(Arquillian.class)
public class CDIInjectionLifecycleEventsIT {

	private static final Logger log = Logger.getLogger( CDIInjectionLifecycleEventsIT.class );

	private static final String MAIN_DEPLOYMENT = "main_deployment";
	private static final String ERROR_CHECKING_DEPLOYMENT = "error_checking_deployment";

	@Deployment(name = MAIN_DEPLOYMENT, managed = false)
	public static Archive<?> createMainArchive() throws Exception {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, CDIInjectionLifecycleEventsIT.class.getSimpleName() + ".war" )
				.addClass( CDIInjectionLifecycleEventsIT.class )
				.addPackages( true /* recursive */, CDIBeansPackage.class.getPackage() )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( "jboss-deployment-structure-hcann.xml", "/jboss-deployment-structure.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchTestingLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return archive;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.jtaDataSource( WildFlyConfigurationExtension.DATA_SOURCE_JNDI_NAME )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Deployment(name = ERROR_CHECKING_DEPLOYMENT, managed = false)
	public static Archive<?> createErrorCheckingArchive() throws Exception {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, CDIInjectionLifecycleEventsIT.class.getSimpleName() + "_errorChecking.war" )
				.addClass( CDIInjectionLifecycleEventsIT.class )
				.addPackage( BridgeCDILifecycleEventCounter.class.getPackage() )
				.addClasses( EventCounts.class, JndiBridgeCDILifecycleEventCounter.class )
				.addAsWebInfResource( "jboss-deployment-structure-hcann.xml", "/jboss-deployment-structure.xml" )
				.addAsLibraries( PackagerHelper.hibernateSearchTestingLibraries() )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return archive;
	}

	@Inject
	private JndiBridgeCDILifecycleEventCounter counter;

	@ArquillianResource
	private Deployer deployer;

	@Test
	@RunAsClient
	@InSequence(0)
	public void deployErrorChecking() {
		deployer.deploy( ERROR_CHECKING_DEPLOYMENT );
	}

	@Test
	@OperateOnDeployment( ERROR_CHECKING_DEPLOYMENT )
	@InSequence(1)
	public void countsInitiallyZero() {
		counter.resetEventCounts();

		log.debug( "Checking whether counts are initially set to 0..." );
		EventCounts eventCounts = counter.getEventCounts();

		assertThat( eventCounts.getClassBridgeConstruct() ).isEqualTo( 0 );
		assertThat( eventCounts.getFieldBridgeConstruct() ).isEqualTo( 0 );
		assertThat( eventCounts.getClassBridgeDestroy() ).isEqualTo( 0 );
		assertThat( eventCounts.getFieldBridgeDestroy() ).isEqualTo( 0 );
	}

	@Test
	@RunAsClient
	@InSequence(2)
	public void deployMain() {
		deployer.deploy( MAIN_DEPLOYMENT );
	}

	@Test
	@OperateOnDeployment( ERROR_CHECKING_DEPLOYMENT )
	@InSequence(3)
	public void fieldBridgeConstructed() {
		log.debug( "Checking whether field bridges were constructed..." );
		EventCounts eventCounts = counter.getEventCounts();

		try {
			// Check that field bridges are created by CDI
			// In particular this means @PostConstruct methods should get called
			// Also, test that one bridge is created per field
			// (Hibernate ORM's bean provider can cache beans in some circumstances, and we don't want that)
			assertThat( eventCounts.getClassBridgeConstruct() ).isEqualTo( 1 );
			assertThat( eventCounts.getFieldBridgeConstruct() ).isEqualTo( 2 );

			// No field bridge should have been destroyed yet
			assertThat( eventCounts.getClassBridgeDestroy() ).isEqualTo( 0 );
			assertThat( eventCounts.getFieldBridgeDestroy() ).isEqualTo( 0 );
		}
		finally {
			counter.resetEventCounts();
		}
	}

	@Test
	@RunAsClient
	@InSequence(4)
	public void undeployMain() {
		// This will trigger application shut down, which should increment the destroy event counts
		deployer.undeploy( MAIN_DEPLOYMENT );
	}

	@Test
	@OperateOnDeployment( ERROR_CHECKING_DEPLOYMENT )
	@InSequence(5)
	public void fieldBridgeDestroyed() {
		log.debug( "Retrieving the result of earlier bridge destruction test..." );
		EventCounts eventCounts = counter.getEventCounts();

		// No more field bridge should have been created
		assertThat( eventCounts.getClassBridgeConstruct() ).isEqualTo( 0 );
		assertThat( eventCounts.getFieldBridgeConstruct() ).isEqualTo( 0 );

		// Check that field bridges are destroyed by CDI
		// In particular this means @PostDestroy methods should get called
		assertThat( eventCounts.getClassBridgeDestroy() ).isEqualTo( 1 );
		assertThat( eventCounts.getFieldBridgeDestroy() ).isEqualTo( 2 );
	}

	@Test
	@RunAsClient
	@InSequence(6)
	public void undeployErrorChecking() {
		deployer.undeploy( ERROR_CHECKING_DEPLOYMENT );
	}

	static class EventCounts implements Serializable {
		private int fieldBridgeConstruct = 0;
		private int fieldBridgeDestroy = 0;
		private int classBridgeConstruct = 0;
		private int classBridgeDestroy = 0;

		int getFieldBridgeConstruct() {
			return fieldBridgeConstruct;
		}

		int getFieldBridgeDestroy() {
			return fieldBridgeDestroy;
		}

		int getClassBridgeConstruct() {
			return classBridgeConstruct;
		}

		int getClassBridgeDestroy() {
			return classBridgeDestroy;
		}

		@Override
		public String toString() {
			return new StringBuilder( "EventCounts[" )
					.append( "fieldBridgeConstruct=" ).append( fieldBridgeConstruct )
					.append( ",fieldBridgeDestroy=" ).append( fieldBridgeDestroy )
					.append( ",classBridgeConstruct=" ).append( classBridgeConstruct )
					.append( ",classBridgeDestroy=" ).append( classBridgeDestroy )
					.append( "]" )
					.toString();
		}
	}

	/**
	 * Implementation notes:
	 * <ul>
	 * <li>this implementation must continue to work even after the Application and Singleton
	 * contexts have been terminated, because it is used in beans that are destroyed later.
	 * Thus we cannot use CDI events, since they would only work while the counter's context is active.
	 * <li>this implementation must save its internal state in order to share it between multiple
	 * deployments, because tests need to look up the event counts after a deployment was shut down.
	 * This save operation must happen after each change, because of the previous bullet point.
	 * </ul>
	 */
	@Dependent
	public static class JndiBridgeCDILifecycleEventCounter implements BridgeCDILifecycleEventCounter {

		private static final Path EVENT_COUNTS_FILE_PATH = Paths.get( System.getProperty( "jboss.server.temp.dir" ) )
				.resolve( JndiBridgeCDILifecycleEventCounter.class.getSimpleName() )
				.resolve( "event_counts.bin" );

		@Override
		public void onFieldBridgeConstruct() {
			log.debug( "Constructing field bridge" );
			withEventCounts( eventCounts -> ++eventCounts.fieldBridgeConstruct );
		}

		@Override
		public void onFieldBridgeDestroy() {
			log.debug( "Destroying field bridge" );
			withEventCounts( eventCounts -> ++eventCounts.fieldBridgeDestroy );
		}

		@Override
		public void onClassBridgeConstruct() {
			log.debug( "Constructing class bridge" );
			withEventCounts( eventCounts -> ++eventCounts.classBridgeConstruct );
		}

		@Override
		public void onClassBridgeDestroy() {
			log.debug( "Destroying class bridge" );
			withEventCounts( eventCounts -> ++eventCounts.classBridgeDestroy );
		}

		EventCounts getEventCounts() {
			return withEventCounts( Function.identity() );
		}

		void resetEventCounts() {
			withEventCounts( eventCounts -> {
				eventCounts.fieldBridgeConstruct = 0;
				eventCounts.fieldBridgeDestroy = 0;
				eventCounts.classBridgeConstruct = 0;
				eventCounts.classBridgeDestroy = 0;
				return null;
			} );
		}

		private <T> T withEventCounts(Function<EventCounts, T> function) {
			try {
				EventCounts eventCounts;

				/*
				 * The event counts cannot be stored as an object in the JNDI context,
				 * because clients use different classloaders and the class definitions
				 * for EventCounts from different classloaders are incompatible.
				 * Thus we serialize everything as bytes, which don't require a shared class definition.
				 */
				if ( Files.exists( EVENT_COUNTS_FILE_PATH ) ) {
					eventCounts = read();
					log.debugf( "Retrieved pre-existing event counts: %s", eventCounts );
				}
				else {
					eventCounts = new EventCounts();
					Files.createDirectories( EVENT_COUNTS_FILE_PATH.getParent() );
					write( eventCounts );
					log.debug( "Could not retrieve pre-existing event counts, created a new instance." );
				}

				try {
					return function.apply( eventCounts );
				}
				finally {
					write( eventCounts );
					log.debugf( "Persisted the current event counts: %s", eventCounts );
				}
			}
			catch (IOException | ClassNotFoundException e) {
				throw new IllegalStateException( e );
			}
		}

		private void write(EventCounts eventCounts) throws IOException {
			try ( OutputStream fileOut = Files.newOutputStream( EVENT_COUNTS_FILE_PATH );
					ObjectOutputStream objectOut = new ObjectOutputStream( fileOut ) ) {
				objectOut.writeObject( eventCounts );
			}
		}

		private EventCounts read() throws IOException, ClassNotFoundException {
			try ( InputStream fileIn = Files.newInputStream( EVENT_COUNTS_FILE_PATH );
					ObjectInputStream objectIn = new ObjectInputStream( fileIn ) ) {
				return (EventCounts) objectIn.readObject();
			}
		}

	}
}
