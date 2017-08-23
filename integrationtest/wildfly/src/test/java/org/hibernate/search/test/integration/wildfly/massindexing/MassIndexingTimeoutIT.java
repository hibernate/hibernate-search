/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.massindexing;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.search.test.integration.VersionTestHelper.getHibernateORMModuleName;
import static org.hibernate.search.test.integration.VersionTestHelper.getWildFlyModuleIdentifier;
import static org.junit.Assert.assertEquals;

/**
 * Test for applying the transaction timeout during id production in mass indexing.
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
@TestForIssue(jiraKey = "HSEARCH-1474")
@ServerSetup(DefaultTransactionTimeoutSetupTask.class)
public class MassIndexingTimeoutIT {

	private static final int NUMBER_OF_ENTIIES = 2000;

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive archive = ShrinkWrap
				.create( WebArchive.class, MassIndexingTimeoutIT.class.getSimpleName() + ".war" )
				.addClasses( Concert.class, ConcertManager.class )
				.addAsResource( persistenceXml(), "META-INF/persistence.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" );
		return archive;
	}

	private static Asset persistenceXml() {
		String persistenceXml = Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.clazz( Concert.class.getName() )
				.jtaDataSource( "java:jboss/datasources/ExampleDS" )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.hbm2ddl.auto" ).value( "create-drop" ).up()
					.createProperty().name( "hibernate.search.default.lucene_version" ).value( "LUCENE_CURRENT" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "local-heap" ).up()
					.createProperty().name( "hibernate.search.indexing_strategy" ).value( "manual" ).up()
					.createProperty().name( "hibernate.jdbc.batch_size" ).value( "50" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( getWildFlyModuleIdentifier() ).up()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( getHibernateORMModuleName() ).up()
				.up().up()
			.exportAsString();
		return new StringAsset( persistenceXml );
	}

	@Inject
	private ConcertManager concertManager;

	/**
	 * Asserts that the configured transaction timeout is applied during id production. The batch size and test data are
	 * chosen to ensure that id consumption applies back-pressure on the {@code ProducerConsumerQueue}, causing the id
	 * producer to run into a TX timeout if the configured default timeout applied.
	 * <p>
	 * More specifically, consumption of the ids takes that long (by means of artificial slow-down in {@link Concert},
	 * that the id producer cannot emit all 2000 ids before the default transaction time out applied. So this test only
	 * passes if the default timeout is overridden by the timeout given for the mass indexer.
	 * <p>
	 * The timeout is given via {@code coordinator-environment#default-timeout} in standalone-full-testqueues.xml.
	 */
	@Test
	public void configuredTimeoutIsAppliedDuringIdProduction() throws Exception {
		insertTestData();

		assertEquals( 0, concertManager.findConcertsByArtist( "Hruce Bronsby" ).size() );

		Concert.SLOW_DOWN = true;
		concertManager.indexConcerts();
		Concert.SLOW_DOWN = false;

		List<Concert> artists = concertManager.findConcertsByArtist( "Hruce Bronsby" );

		assertEquals(
				"Expecting all entries to be indexed, as the configured transaction timeout is long enough  to " +
						"produce all items",
				NUMBER_OF_ENTIIES,
				artists.size()
		);
	}

	private void insertTestData() {
		List<Concert> concerts = new ArrayList<>();
		GregorianCalendar calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
		calendar.set( 2015, 0, 31 );
		Date time = calendar.getTime();
		for ( int i = 0; i < NUMBER_OF_ENTIIES; i++ ) {
			concerts.add( new Concert( "Hruce Bronsby", time ) );
		}

		concertManager.saveConcerts( concerts );
	}
}
